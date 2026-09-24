package com.infy.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.infy.service.AlarmService;

/**
 * BE US16 Simulator: on a fixed delay, generates alarm XML and ingests it via
 * AlarmService. Disabled unless app.simulator.enabled=true. Exceptions are
 * caught so one bad run never cancels the schedule.
 */
@Component
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class AlarmSimulatorJob {

    private static final Logger logger = LoggerFactory.getLogger(AlarmSimulatorJob.class);

    @Autowired
    private AlarmXmlGenerator generator;

    @Autowired
    private AlarmService alarmService;

    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:30000}",
               initialDelayString = "${app.simulator.initial-delay-ms:10000}")
    public void run() {
        try {
            String xml = generator.generate();
            if (xml == null) {
                logger.info("Simulator run skipped - no active devices");
                return;
            }
            int ingested = alarmService.ingestAlarmsFromXml(xml);
            logger.info("Simulator run ingested {} alarm(s)", ingested);
        } catch (Exception ex) {
            logger.error("Simulator run failed", ex);
        }
    }
}
