package com.newsprocessor.service;

import com.newsprocessor.domain.NewsItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * V1 fingerprinting and V2 story-cluster metric helpers.
 */
final class NewsStorySupport {

    static final double STORY_SIMILARITY_THRESHOLD = 0.55;
    static final int CLUSTER_LOOKBACK_HOURS = 48;

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private NewsStorySupport() {
    }

    static String normalizeHeadline(String headline) {
        if (headline == null || headline.isBlank()) {
            return "";
        }
        String lower = headline.toLowerCase(Locale.ROOT);
        String stripped = NON_ALNUM.matcher(lower).replaceAll(" ");
        return MULTI_SPACE.matcher(stripped).replaceAll(" ").trim();
    }

    static String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        return MULTI_SPACE.matcher(source.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    /** V1 hard-dedupe key: ticker + source + normalized headline. */
    static String fingerprint(String ticker, String source, String headline) {
        String payload = String.join(
                "|",
                ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT),
                normalizeSource(source),
                normalizeHeadline(headline));
        return sha256Hex(payload);
    }

    static double jaccardSimilarity(String headlineA, String headlineB) {
        Set<String> a = tokens(normalizeHeadline(headlineA));
        Set<String> b = tokens(normalizeHeadline(headlineB));
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /** Item hotness / overall-hotness tier weight: Tier1=3, Tier2=2, Tier3=1. */
    static int tierHotnessWeight(String mediaTier) {
        if (mediaTier == null) {
            return 1;
        }
        String value = mediaTier.toLowerCase(Locale.ROOT);
        if (value.contains("1")) {
            return 3;
        }
        if (value.contains("3")) {
            return 1;
        }
        if (value.contains("2")) {
            return 2;
        }
        return 1;
    }

    /** Sentiment aggregation weight aligned with NSI prompt: 1.5 / 1.0 / 1.2. */
    static double tierSentimentWeight(String mediaTier) {
        if (mediaTier == null) {
            return 1.0;
        }
        String value = mediaTier.toLowerCase(Locale.ROOT);
        if (value.contains("1")) {
            return 1.5;
        }
        if (value.contains("3")) {
            return 1.2;
        }
        if (value.contains("2")) {
            return 1.0;
        }
        return 1.0;
    }

    /**
     * One representative item per distinct source (latest by createdAt, then id).
     */
    static List<NewsItem> latestPerSource(List<NewsItem> clusterItems) {
        Map<String, NewsItem> bySource = new HashMap<>();
        List<NewsItem> ordered = new ArrayList<>(clusterItems);
        ordered.sort(Comparator
                .comparing(NewsItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(NewsItem::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        for (NewsItem item : ordered) {
            String key = normalizeSource(item.getSource());
            if (key.isEmpty()) {
                key = "unknown-" + (item.getId() == null ? bySource.size() : item.getId());
            }
            bySource.put(key, item);
        }
        return List.copyOf(bySource.values());
    }

    /** Overall story hotness: sum of tier hotness weights across distinct sources. */
    static int computeOverallHotness(List<NewsItem> clusterItems) {
        int total = 0;
        for (NewsItem item : latestPerSource(clusterItems)) {
            total += tierHotnessWeight(item.getMediaTier());
        }
        return Math.max(total, 1);
    }

    /**
     * Tier-weighted average of member scores (−100…+100), one latest item per source.
     */
    static Double computeAggregateSentiment(List<NewsItem> clusterItems) {
        List<NewsItem> reps = latestPerSource(clusterItems);
        if (reps.isEmpty()) {
            return null;
        }
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        for (NewsItem item : reps) {
            if (item.getScore() == null) {
                continue;
            }
            double weight = tierSentimentWeight(item.getMediaTier());
            weightedSum += weight * item.getScore();
            weightTotal += weight;
        }
        if (weightTotal == 0.0) {
            return null;
        }
        return weightedSum / weightTotal;
    }

    static NewsItem pickCanonicalItem(List<NewsItem> clusterItems) {
        return clusterItems.stream()
                .max(Comparator
                        .comparingInt((NewsItem item) -> tierHotnessWeight(item.getMediaTier()))
                        .thenComparing(NewsItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(NewsItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static Set<String> tokens(String normalizedHeadline) {
        if (normalizedHeadline.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(normalizedHeadline.split(" "))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
