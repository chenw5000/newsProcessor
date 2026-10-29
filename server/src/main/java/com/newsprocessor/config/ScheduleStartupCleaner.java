package com.newsprocessor.config;

import com.newsprocessor.service.schedule.AnalyzeScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Clears a leftover in-progress lock after process restart so the scheduler is not stuck.
 */
@Component
public class ScheduleStartupCleaner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduleStartupCleaner.class);

    private final AnalyzeScheduleService scheduleService;

    public ScheduleStartupCleaner(AnalyzeScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Clearing schedule run lock on startup (if any)");
        scheduleService.clearLockOnStartup();
    }
}
