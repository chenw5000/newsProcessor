package com.newsprocessor.web.dto;

import java.util.List;

public class AnalyzeNewsResponse {

    private List<Long> batchIds;
    private int itemCount;
    private int skippedDuplicateCount;
    private int clusteredCount;
    private int maxOverallHotness;
    private Double minAggregateSentiment;
    private Double maxAggregateSentiment;

    public AnalyzeNewsResponse() {
    }

    public AnalyzeNewsResponse(
            List<Long> batchIds,
            int itemCount,
            int skippedDuplicateCount,
            int clusteredCount,
            int maxOverallHotness,
            Double minAggregateSentiment,
            Double maxAggregateSentiment) {
        this.batchIds = batchIds;
        this.itemCount = itemCount;
        this.skippedDuplicateCount = skippedDuplicateCount;
        this.clusteredCount = clusteredCount;
        this.maxOverallHotness = maxOverallHotness;
        this.minAggregateSentiment = minAggregateSentiment;
        this.maxAggregateSentiment = maxAggregateSentiment;
    }

    public List<Long> getBatchIds() {
        return batchIds;
    }

    public void setBatchIds(List<Long> batchIds) {
        this.batchIds = batchIds;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getSkippedDuplicateCount() {
        return skippedDuplicateCount;
    }

    public void setSkippedDuplicateCount(int skippedDuplicateCount) {
        this.skippedDuplicateCount = skippedDuplicateCount;
    }

    public int getClusteredCount() {
        return clusteredCount;
    }

    public void setClusteredCount(int clusteredCount) {
        this.clusteredCount = clusteredCount;
    }

    public int getMaxOverallHotness() {
        return maxOverallHotness;
    }

    public void setMaxOverallHotness(int maxOverallHotness) {
        this.maxOverallHotness = maxOverallHotness;
    }

    public Double getMinAggregateSentiment() {
        return minAggregateSentiment;
    }

    public void setMinAggregateSentiment(Double minAggregateSentiment) {
        this.minAggregateSentiment = minAggregateSentiment;
    }

    public Double getMaxAggregateSentiment() {
        return maxAggregateSentiment;
    }

    public void setMaxAggregateSentiment(Double maxAggregateSentiment) {
        this.maxAggregateSentiment = maxAggregateSentiment;
    }
}
