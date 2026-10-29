package com.newsprocessor.web;

import com.newsprocessor.service.GeminiNewsPersistResult;
import com.newsprocessor.service.GeminiNewsService;
import com.newsprocessor.service.schedule.AnalyzeScheduleService;
import com.newsprocessor.web.dto.AnalyzeNewsRequest;
import com.newsprocessor.web.dto.AnalyzeNewsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final GeminiNewsService geminiNewsService;
    private final AnalyzeScheduleService scheduleService;

    public NewsController(GeminiNewsService geminiNewsService, AnalyzeScheduleService scheduleService) {
        this.geminiNewsService = geminiNewsService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("/analyze")
    public AnalyzeNewsResponse analyze(@Valid @RequestBody AnalyzeNewsRequest request) {
        return toResponse(geminiNewsService.analyzeAndStore(request.getTickers()));
    }

    /**
     * Analyzes all tickers in {@code stock} with {@code enabled = true}.
     * Uses the same run lock as the scheduler so it will not overlap an in-progress job.
     */
    @PostMapping("/analyze-enabled")
    public AnalyzeNewsResponse analyzeEnabled() {
        return toResponse(scheduleService.runManualAnalyzeEnabled());
    }

    private static AnalyzeNewsResponse toResponse(GeminiNewsPersistResult result) {
        return new AnalyzeNewsResponse(
                result.getBatchIds(),
                result.getItemCount(),
                result.getSkippedDuplicateCount(),
                result.getClusteredCount(),
                result.getMaxOverallHotness(),
                result.getMinAggregateSentiment(),
                result.getMaxAggregateSentiment());
    }
}
