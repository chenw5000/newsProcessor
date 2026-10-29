package com.newsprocessor.web;

import com.newsprocessor.service.schedule.AnalyzeScheduleService;
import com.newsprocessor.service.schedule.ScheduleConfig;
import com.newsprocessor.web.dto.ScheduleStatusResponse;
import com.newsprocessor.web.dto.ScheduleUpdateRequest;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final AnalyzeScheduleService scheduleService;

    public ScheduleController(AnalyzeScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ScheduleStatusResponse get() {
        return toResponse(scheduleService.getConfig());
    }

    @PutMapping
    public ScheduleStatusResponse update(@RequestBody ScheduleUpdateRequest request) {
        ScheduleConfig updated = scheduleService.updateConfig(
                request.getEnabled(),
                request.getTimezone(),
                request.getWindowStart(),
                request.getWindowEnd(),
                request.getIntervalMinutes(),
                request.getChunkSize(),
                request.getChunkPauseSeconds());
        return toResponse(updated);
    }

    private static ScheduleStatusResponse toResponse(ScheduleConfig config) {
        ScheduleStatusResponse response = new ScheduleStatusResponse();
        response.setEnabled(config.enabled());
        response.setTimezone(config.timezone().getId());
        response.setWindowStart(config.windowStart().format(TIME));
        response.setWindowEnd(config.windowEnd().format(TIME));
        response.setIntervalMinutes(config.intervalMinutes());
        response.setChunkSize(config.chunkSize());
        response.setChunkPauseSeconds(config.chunkPauseSeconds());
        response.setLastRunAt(config.lastRunAt() == null ? null : config.lastRunAt().toString());
        response.setRunInProgress(config.runInProgress());
        response.setRunStartedAt(config.runStartedAt() == null ? null : config.runStartedAt().toString());
        response.setLastRunStatus(config.lastRunStatus());
        response.setLastError(config.lastError());
        return response;
    }
}
