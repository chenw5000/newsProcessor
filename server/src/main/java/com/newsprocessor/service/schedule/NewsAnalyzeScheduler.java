package com.newsprocessor.service.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cheap periodic tick that evaluates DB-backed schedule settings and may run
 * {@code analyzeEnabledStocks()}. Change frequency/window/enabled in {@code app_setting}
 * without restarting the process.
 */
@Component
public class NewsAnalyzeScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsAnalyzeScheduler.class);

    private final AnalyzeScheduleService scheduleService;

    public NewsAnalyzeScheduler(AnalyzeScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Scheduled(fixedDelayString = "${news.schedule.tick-ms:30000}")
    public void tick() {
        try {
            scheduleService.tick();
        } catch (RuntimeException exception) {
            // Failures are recorded in app_setting; keep the ticker alive.
            log.warn("Schedule tick failed: {}", exception.getMessage());
        }
    }
}
