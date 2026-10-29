package com.newsprocessor.web.dto;

import java.util.List;

public class AnalyzeNewsResponse {

    private List<Long> batchIds;
    private int itemCount;
    private int skippedDuplicateCount;
    private int clusteredCount;
    private int maxHotness;

    public AnalyzeNewsResponse() {
    }

    public AnalyzeNewsResponse(
            List<Long> batchIds,
            int itemCount,
            int skippedDuplicateCount,
            int clusteredCount,
            int maxHotness) {
        this.batchIds = batchIds;
        this.itemCount = itemCount;
        this.skippedDuplicateCount = skippedDuplicateCount;
        this.clusteredCount = clusteredCount;
        this.maxHotness = maxHotness;
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

    public int getMaxHotness() {
        return maxHotness;
    }

    public void setMaxHotness(int maxHotness) {
        this.maxHotness = maxHotness;
    }
}
