package com.newsprocessor.service;

import java.util.ArrayList;
import java.util.List;

public class GeminiNewsPersistResult {

    private final List<Long> batchIds;
    private final int itemCount;
    private final int skippedDuplicateCount;
    private final int clusteredCount;
    private final int maxOverallHotness;
    private final Double minAggregateSentiment;
    private final Double maxAggregateSentiment;

    public GeminiNewsPersistResult(
            List<Long> batchIds,
            int itemCount,
            int skippedDuplicateCount,
            int clusteredCount,
            int maxOverallHotness,
            Double minAggregateSentiment,
            Double maxAggregateSentiment) {
        this.batchIds = List.copyOf(batchIds);
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

    public int getItemCount() {
        return itemCount;
    }

    public int getSkippedDuplicateCount() {
        return skippedDuplicateCount;
    }

    public int getClusteredCount() {
        return clusteredCount;
    }

    public int getMaxOverallHotness() {
        return maxOverallHotness;
    }

    /** @deprecated use {@link #getMaxOverallHotness()} */
    @Deprecated
    public int getMaxHotness() {
        return maxOverallHotness;
    }

    public Double getMinAggregateSentiment() {
        return minAggregateSentiment;
    }

    public Double getMaxAggregateSentiment() {
        return maxAggregateSentiment;
    }

    public static GeminiNewsPersistResult merge(GeminiNewsPersistResult left, GeminiNewsPersistResult right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        ArrayList<Long> batchIds = new ArrayList<>(left.batchIds);
        batchIds.addAll(right.batchIds);
        Double minSentiment = minNullable(left.minAggregateSentiment, right.minAggregateSentiment);
        Double maxSentiment = maxNullable(left.maxAggregateSentiment, right.maxAggregateSentiment);
        return new GeminiNewsPersistResult(
                batchIds,
                left.itemCount + right.itemCount,
                left.skippedDuplicateCount + right.skippedDuplicateCount,
                left.clusteredCount + right.clusteredCount,
                Math.max(left.maxOverallHotness, right.maxOverallHotness),
                minSentiment,
                maxSentiment);
    }

    private static Double minNullable(Double a, Double b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.min(a, b);
    }

    private static Double maxNullable(Double a, Double b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.max(a, b);
    }
}
