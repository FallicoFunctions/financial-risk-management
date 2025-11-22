package com.nickfallico.financialriskmanagement.config;

import com.nickfallico.financialriskmanagement.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled metrics tasks.
 * Handles periodic metrics updates and resets.
 */
@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class MetricsSchedulerConfig {

    private final MetricsService metricsService;

    /**
     * Reset hourly transaction counters every hour.
     * Runs at the start of each hour (00 minutes, 00 seconds).
     */
    @Scheduled(cron = "0 0 * * * *")
    public void resetHourlyCounters() {
        log.info("Resetting hourly transaction counters");
        metricsService.resetHourlyCounters();
    }

    /**
     * Update fraud detection rate every 5 minutes.
     * This provides near real-time fraud rate monitoring.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void updateFraudDetectionRate() {
        metricsService.updateFraudDetectionRate();
        log.debug("Updated fraud detection rate metric");
    }
}
