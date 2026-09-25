package com.infy.simulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.infy.service.AlarmService;

/**
 * BE US16 Simulator, ingestion half. Runs on its own fixed schedule, offset
 * from AlarmGenerationJob's so a file is always guaranteed to exist before
 * this job goes looking for one.
 *
 * Claim-then-process protocol (prevents double ingestion -- and therefore a
 * duplicated `occurrence` bump -- if two runs ever overlap):
 *   pending/  -> [claim: atomic move]   -> processing/
 *   processing/ -> [ingest succeeds]    -> processed/
 *   processing/ -> [malformed XML]      -> failed/            (permanent failure)
 *   processing/ -> [DB/IO/other error]  -> pending/            (temporary failure, retried next run)
 *
 * A file is only ever read out of processing/, never directly out of
 * pending/, so a claimed file can't be picked up by a second concurrent run.
 * One file's failure never stops the rest of the batch. Every move result is
 * checked; nothing is logged as "ingested" until the file has actually
 * landed in processed/. Disabled unless app.simulator.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class AlarmIngestionJob {

    private static final Logger logger = LoggerFactory.getLogger(AlarmIngestionJob.class);

    @Autowired
    private AlarmService alarmService;

    @Value("${app.simulator.pending-dir:./simulator-data/pending}")
    private String pendingDir;

    @Value("${app.simulator.processing-dir:./simulator-data/processing}")
    private String processingDir;

    @Value("${app.simulator.processed-dir:./simulator-data/processed}")
    private String processedDir;

    @Value("${app.simulator.failed-dir:./simulator-data/failed}")
    private String failedDir;

    @Scheduled(fixedDelayString = "${app.simulator.ingestion-interval-ms:60000}",
               initialDelayString = "${app.simulator.ingestion-initial-delay-ms:10000}")
    public void run() {
        Path pending = Path.of(pendingDir);
        if (!Files.isDirectory(pending)) {
            logger.info("Simulator ingestion run skipped - pending directory does not exist yet: {}", pending);
            return;
        }

        List<Path> files = listPendingXmlFilesOldestFirst(pending);
        if (files.isEmpty()) {
            logger.info("Simulator ingestion run found no pending files");
            return;
        }

        int processedCount = 0;
        for (Path pendingFile : files) {
            // Claim first: atomically move out of pending/ so no other run (this
            // job's next tick, or a concurrent instance) can pick up the same file.
            Path claimed;
            try {
                claimed = moveFile(pendingFile, Path.of(processingDir));
            } catch (IOException ex) {
                logger.error("Failed to claim pending file {} (moving pending/ -> processing/); " +
                        "leaving it in pending/ for a later run", pendingFile.getFileName(), ex);
                continue;
            }

            processedCount += processClaimedFile(claimed) ? 1 : 0;
        }
        // totalIngested intentionally not tracked across the loop above beyond logging per-file;
        // the per-run summary reports files attempted, since a partial batch can straddle
        // processed/failed/retried outcomes.
        logger.info("Simulator ingestion run attempted {} file(s), {} fully processed", files.size(), processedCount);
    }

    /**
     * @return true if the claimed file reached processed/ (a genuine success); false for any
     * other outcome (malformed -> failed/, temporary error -> back to pending/, or a move
     * itself failing, all of which are logged individually below).
     */
    private boolean processClaimedFile(Path claimedFile) {
        String xml;
        try {
            xml = Files.readString(claimedFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Failed to read claimed file {} - treating as a temporary failure", claimedFile.getFileName(), ex);
            retryLater(claimedFile);
            return false;
        }

        int ingested;
        try {
            ingested = alarmService.ingestAlarmsFromXml(xml);
        } catch (IllegalArgumentException ex) {
            // AlarmXmlParser throws IllegalArgumentException for malformed/unparseable XML --
            // that will never succeed on retry, so it's a permanent failure.
            logger.error("Malformed alarm XML in {} - quarantining to failed/", claimedFile.getFileName(), ex);
            archiveAsFailed(claimedFile);
            return false;
        } catch (Exception ex) {
            // DB errors, transient I/O, or anything else not attributable to the XML content
            // itself -- retry on a later run rather than discarding the batch.
            logger.error("Temporary failure ingesting {} - returning it to pending/ for retry", claimedFile.getFileName(), ex);
            retryLater(claimedFile);
            return false;
        }

        try {
            moveFile(claimedFile, Path.of(processedDir));
        } catch (IOException ex) {
            // The alarms are already committed to the DB at this point -- ingestion itself
            // succeeded. Only the archival move failed. Do NOT report this as "ingested
            // successfully" (the file is still sitting, unmoved, in processing/), and do NOT
            // route it to pending/ or failed/: re-ingesting it would double the occurrence
            // count / duplicate rows for a batch that's already in the DB. Surface it loudly
            // so it can be moved by hand; the next run will simply skip over it since it's
            // no longer in pending/.
            logger.error("Alarms from {} were ingested successfully ({} alarm(s)), but moving the file " +
                    "processing/ -> processed/ failed. The file remains in processing/ and needs manual " +
                    "attention - it must NOT be re-ingested.", claimedFile.getFileName(), ingested, ex);
            return false;
        }

        logger.info("Ingested {} alarm(s) from {} and archived it to processed/", ingested, claimedFile.getFileName());
        return true;
    }

    /** Moves a claimed file back to pending/ for retry; logs loudly if even that move fails. */
    private void retryLater(Path claimedFile) {
        try {
            moveFile(claimedFile, Path.of(pendingDir));
        } catch (IOException ex) {
            logger.error("Failed to return {} from processing/ to pending/ for retry - it remains stuck " +
                    "in processing/ and needs manual attention", claimedFile.getFileName(), ex);
        }
    }

    /** Moves a claimed file to failed/ for a permanent (non-retryable) failure; logs loudly if the move fails. */
    private void archiveAsFailed(Path claimedFile) {
        try {
            moveFile(claimedFile, Path.of(failedDir));
        } catch (IOException ex) {
            logger.error("Failed to move {} from processing/ to failed/ - it remains stuck in processing/ " +
                    "and needs manual attention", claimedFile.getFileName(), ex);
        }
    }

    /** Only *.xml is matched, so a stray .tmp left by a crashed generation write is never picked up. */
    private List<Path> listPendingXmlFilesOldestFirst(Path pending) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pending, "*.xml")) {
            for (Path p : stream) {
                files.add(p);
            }
        } catch (IOException ex) {
            logger.error("Simulator ingestion run failed to list pending directory: {}", pending, ex);
        }
        // Filenames are "alarms-<yyyyMMdd-HHmmss-SSS>-<seq>.xml" (AlarmGenerationJob), so
        // lexicographic filename order is chronological order -- oldest first.
        files.sort(Comparator.comparing(Path::getFileName));
        return files;
    }

    /**
     * Atomically moves a file into targetDir (creating it if needed), falling back to a
     * non-atomic move only on filesystems that don't support ATOMIC_MOVE. Throws IOException
     * on any failure instead of swallowing it, so every caller must explicitly decide what a
     * failed move means for that file.
     *
     * @return the file's new path inside targetDir
     */
    private Path moveFile(Path source, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(source.getFileName());
        try {
            return Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
        	return Files.move(source, target);
        }
    }
}
