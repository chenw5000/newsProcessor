package com.newsprocessor.web;

import com.newsprocessor.service.GeminiNewsPersistResult;
import com.newsprocessor.service.GeminiNewsService;
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

    public NewsController(GeminiNewsService geminiNewsService) {
        this.geminiNewsService = geminiNewsService;
    }

    @PostMapping("/analyze")
    public AnalyzeNewsResponse analyze(@Valid @RequestBody AnalyzeNewsRequest request) {
        GeminiNewsPersistResult result = geminiNewsService.analyzeAndStore(request.getTickers());
        return new AnalyzeNewsResponse(
                result.getBatchIds(),
                result.getItemCount(),
                result.getSkippedDuplicateCount(),
                result.getClusteredCount(),
                result.getMaxHotness());
    }
}
