package com.newsprocessor.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class AnalyzeNewsRequest {

    @NotEmpty
    private List<String> tickers;

    public List<String> getTickers() {
        return tickers;
    }

    public void setTickers(List<String> tickers) {
        this.tickers = tickers;
    }
}
