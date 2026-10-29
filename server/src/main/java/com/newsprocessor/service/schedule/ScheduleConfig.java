package com.newsprocessor.service.schedule;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public record ScheduleConfig(
        boolean enabled,
        ZoneId timezone,
        LocalTime windowStart,
        LocalTime windowEnd,
        int intervalMinutes,
        int chunkSize,
        int chunkPauseSeconds,
        Instant lastRunAt,
        boolean runInProgress,
        Instant runStartedAt,
        String lastRunStatus,
        String lastError) {
}
