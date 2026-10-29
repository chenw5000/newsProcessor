package com.newsprocessor.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * V1 fingerprinting and V2 near-duplicate / hotness helpers for news items.
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

    static int tierWeight(String mediaTier) {
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

    /**
     * Basic hotness: sum of tier weights across distinct normalized sources in a cluster.
     * One outlet counted once even if it appears multiple times.
     */
    static int computeHotness(Iterable<String> sources, Iterable<String> mediaTiers) {
        Set<String> seen = new HashSet<>();
        int total = 0;
        var sourceIt = sources.iterator();
        var tierIt = mediaTiers.iterator();
        while (sourceIt.hasNext() && tierIt.hasNext()) {
            String sourceKey = normalizeSource(sourceIt.next());
            String tier = tierIt.next();
            if (sourceKey.isEmpty()) {
                sourceKey = "unknown-" + seen.size();
            }
            if (seen.add(sourceKey)) {
                total += tierWeight(tier);
            }
        }
        return Math.max(total, 1);
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
