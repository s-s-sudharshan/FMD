package com.infy.simulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BE US16 Simulator, generation half. On a fixed schedule, builds one alarm
 * XML entry per currently ACTIVATED device (AlarmXmlGenerator) and writes
 * the batch as a timestamped file into the "pending" directory for
 * AlarmIngestionJob to pick up on its own schedule.
 *
 * Written via temp-file-then-atomic-rename so the ingestion job -- running
 * as an independently scheduled job against the same directory -- can never
 * observe a partially-written file. Disabled unless app.simulator.enabled=true.
 * Exceptions are caught so one bad run never cancels the schedule.
 */
@Component
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class AlarmGenerationJob {

    private static final Logger logger = LoggerFactory.getLogger(AlarmGenerationJob.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    /** Disambiguates two files whose formatted timestamp collides (same millisecond bucket). */
    private static final AtomicLong SEQUENCE = new AtomicLong();

    @Autowired
    private AlarmXmlGenerator generator;

    @Value("${app.simulator.pending-dir:./simulator-data/pending}")
    private String pendingDir;

    @Scheduled(fixedDelayString = "${app.simulator.generation-interval-ms:60000}",
               initialDelayString = "${app.simulator.generation-initial-delay-ms:0}")
    public void run() {
        try {
            String xml = generator.generate();
            if (xml == null) {
                logger.info("Simulator generation run skipped - no active devices");
                return;
            }

            Path dir = Path.of(pendingDir);
            Files.createDirectories(dir);

            String fileName = "alarms-" + LocalDateTime.now().format(TIMESTAMP_FORMAT)
                    + "-" + SEQUENCE.incrementAndGet() + ".xml";
            Path target = dir.resolve(fileName);
            Path tempFile = dir.resolve(fileName + ".tmp");

            Files.writeString(tempFile, xml, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, target);
            }

            logger.info("Simulator generation run wrote {}", target);
        } catch (IOException ex) {
            logger.error("Simulator generation run failed to write pending file", ex);
        } catch (Exception ex) {
            logger.error("Simulator generation run failed", ex);
        }
    }
}
