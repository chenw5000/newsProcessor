package com.newsprocessor.service;

import java.util.List;

public class GeminiNewsPersistResult {

    private final List<Long> batchIds;
    private final int itemCount;
    private final int skippedDuplicateCount;
    private final int clusteredCount;
    private final int maxHotness;

    public GeminiNewsPersistResult(
            List<Long> batchIds,
            int itemCount,
            int skippedDuplicateCount,
            int clusteredCount,
            int maxHotness) {
        this.batchIds = List.copyOf(batchIds);
        this.itemCount = itemCount;
        this.skippedDuplicateCount = skippedDuplicateCount;
        this.clusteredCount = clusteredCount;
        this.maxHotness = maxHotness;
    }

    public List<Long> getBatchIds() {
        return batchIds;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getSkippedDuplicateCount() {
        return skippedDuplicateCount;
    }

    public int getClusteredCount() {
        return clusteredCount;
    }

    public int getMaxHotness() {
        return maxHotness;
    }
}
