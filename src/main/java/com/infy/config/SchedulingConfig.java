package com.infy.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled processing only when the simulator is enabled. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class SchedulingConfig {
}
