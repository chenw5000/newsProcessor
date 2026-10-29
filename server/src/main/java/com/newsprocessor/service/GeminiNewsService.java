package com.newsprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.newsprocessor.config.GeminiProperties;
import com.newsprocessor.domain.NewsBatch;
import com.newsprocessor.domain.NewsItem;
import com.newsprocessor.domain.NewsStoryCluster;
import com.newsprocessor.domain.Stock;
import com.newsprocessor.domain.AppSetting;
import com.newsprocessor.repo.AppSettingRepository;
import com.newsprocessor.repo.NewsBatchRepository;
import com.newsprocessor.repo.NewsItemRepository;
import com.newsprocessor.repo.NewsStoryClusterRepository;
import com.newsprocessor.repo.StockRepository;
import com.newsprocessor.service.schedule.AnalyzeScheduleService;
import com.newsprocessor.service.dto.GeminiNewsResponse;
import com.newsprocessor.service.dto.GeminiNewsResponse.HorizonWindow;
import com.newsprocessor.service.dto.GeminiNewsResponse.NewsEntry;
import com.newsprocessor.service.dto.GeminiNewsResponse.NormalizedIndex;
import com.newsprocessor.service.dto.GeminiNewsResponse.TickerNews;
import com.newsprocessor.service.dto.GeminiNewsResponse.TimeHorizonOutlook;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeminiNewsService {

    private static final Logger log = LoggerFactory.getLogger(GeminiNewsService.class);
    private static final String TICKER_PLACEHOLDER = "[INSERT LIST OF TICKERS, e.g., NVDA, AAPL]";
    private static final String EXCLUSION_PLACEHOLDER = "{{ALREADY_COVERED_SECTION}}";
    private static final Duration EXCLUSION_LOOKBACK = Duration.ofHours(2);
    private static final int EXCLUSION_LAST_BATCHES = 2;
    private static final DateTimeFormatter RELEASE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROMPT_TEMPLATE = """
            Act as an advanced quantitative financial analyst and sentiment engine.

            I want you to analyze the recent news regarding [INSERT LIST OF TICKERS, e.g., NVDA, AAPL] from major financial news resources.

            Please perform the following steps for each asset:

            1. NEWS AGGREGATION & SOURCE ATTRIBUTION:
            Gather ONLY verified, real-world headlines from the past 24 hours. Do not simulate, guess, or fabricate any news items. Prefer up to 4-6 NEW items if available. You must classify each source according to this strict media tier framework:
            - Tier 1: Institutional Wires (Reuters, Bloomberg, WSJ, Dow Jones Newswires)
            - Tier 2: Mainstream Outlets (CNBC, Yahoo Finance, Financial Times)
            - Tier 3: Sector Specialists (The Information, DigiTimes, Benzinga, MarketWatch)

            1b. ALREADY COVERED / NO FILLERS:
            Below is a list of source+headline pairs already collected recently for these tickers. Do NOT repeat them (exact match, paraphrase, or the same underlying story). Prefer other distinct verified stories from the past 24 hours.
            If nothing new exists beyond the exclusion list, return fewer items or an empty news array for that ticker. Do NOT invent, guess, or rephrase excluded headlines to pad the list.
            {{ALREADY_COVERED_SECTION}}

            2. INDIVIDUAL SENTIMENT SCORING:
            Rate the potential market impact of EACH news item individually on a raw scale from -100 (extreme panic/bearish) to +100 (extreme euphoria/bullish). Justify the score based on institutional market reactions.

            3. TOTAL NORMALIZED EFFECT VALUE (WEIGHTED INDEX):
            Calculate a single "Normalized Sentiment Index" (NSI) on a strict scale of -1.0 to +1.0.
            You must mathematically calculate the score by applying the following source weights to the individual news scores:
            - Tier 1 (Institutional Wires) = 1.5x Weight
            - Tier 2 (Mainstream Outlets) = 1.0x Weight (Baseline)
            - Tier 3 (Sector Specialists) = 1.2x Weight
            *Note: If no news items are found for a stock, return a score of 0.0.*

            4. TIME-HORIZON BREAKDOWN:
            Map how this normalized value will affect the price across three windows: Immediate (Next open), 10 Days, and 30 Days. Provide a directional impact score for each window scaled from -100 (extreme downward pressure) to +100 (extreme upward momentum), with 0 representing zero net price impact.

            OUTPUT FORMAT:
            Provide the entire response strictly in a valid JSON format following this exact schema structure. Do not include any markdown conversational filler outside of the JSON block:

            {
              "news_items": [
                {
                  "ticker": "STOCK_TICKER_HERE",
                  "news": [
                    {
                      "source": "Source Name",
                      "media_tier": "Tier 1 / Tier 2 / Tier 3",
                      "release_time": "Release Time in yyyy-MM-dd HH:mm:ss",
                      "headline": "Headline Text",
                      "summary": "Brief Summary Text",
                      "core_theme": "Core Theme",
                      "score": 0,
                      "justification": "Justification text here"
                    }
                  ],
                  "normalized_index": {
                    "score": 0.00,
                    "logic": "Detailed breakdown of the weighted calculation and tier reasoning here"
                  },
                  "time_horizon_outlook": {
                    "immediate": {
                      "score": 0,
                      "description": "Immediate impact description"
                    },
                    "10_days": {
                      "score": 0,
                      "description": "10 days impact description"
                    },
                    "30_days": {
                      "score": 0,
                      "description": "30 days impact description"
                    }
                  }
                }
              ]
            }
            """;

    private static final int DEFAULT_CHUNK_SIZE = 8;
    private static final int DEFAULT_CHUNK_PAUSE_SECONDS = 30;

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final StockRepository stocks;
    private final NewsBatchRepository batches;
    private final NewsItemRepository items;
    private final NewsStoryClusterRepository storyClusters;
    private final AppSettingRepository appSettings;
    private final TransactionTemplate transactionTemplate;

    public GeminiNewsService(
            GeminiProperties geminiProperties,
            StockRepository stocks,
            NewsBatchRepository batches,
            NewsItemRepository items,
            NewsStoryClusterRepository storyClusters,
            AppSettingRepository appSettings,
            PlatformTransactionManager transactionManager) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = new ObjectMapper();
        this.stocks = stocks;
        this.batches = batches;
        this.items = items;
        this.storyClusters = storyClusters;
        this.appSettings = appSettings;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public String buildPrompt(List<String> tickers) {
        return PROMPT_TEMPLATE
                .replace(TICKER_PLACEHOLDER, formatTickers(tickers))
                .replace(EXCLUSION_PLACEHOLDER, buildExclusionSection(tickers));
    }

    /**
     * Builds the ALREADY COVERED block: source+headline from items in the last 2 hours
     * or belonging to the last 2 batches per ticker.
     */
    String buildExclusionSection(List<String> tickers) {
        Instant since = Instant.now().minus(EXCLUSION_LOOKBACK);
        StringBuilder section = new StringBuilder("ALREADY COVERED (source | headline):");
        boolean any = false;
        for (String ticker : tickers) {
            List<String> lines = loadExclusionLines(ticker, since);
            if (lines.isEmpty()) {
                continue;
            }
            any = true;
            section.append('\n').append(ticker).append(':');
            for (String line : lines) {
                section.append("\n- ").append(line);
            }
        }
        if (!any) {
            return "ALREADY COVERED (source | headline):\nnone";
        }
        return section.toString();
    }

    private List<String> loadExclusionLines(String ticker, Instant since) {
        Stock stock = stocks.findFirstByTickerIgnoreCase(ticker).orElse(null);
        if (stock == null || stock.getId() == null) {
            return List.of();
        }
        Integer tickerId = toTickerId(stock.getId());
        List<Long> recentBatchIds = batches.findIdsByTickerIdOrderByIdDesc(
                tickerId, PageRequest.of(0, EXCLUSION_LAST_BATCHES));

        List<NewsItem> candidates;
        if (recentBatchIds.isEmpty()) {
            candidates = items.findRecentByTickerId(tickerId, since);
        } else {
            candidates = items.findExclusionCandidates(tickerId, since, recentBatchIds);
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> lines = new ArrayList<>();
        for (NewsItem item : candidates) {
            String source = blankToNull(item.getSource());
            String headline = blankToNull(item.getHeadline());
            if (source == null || headline == null) {
                continue;
            }
            String fingerprint = item.getHeadlineFingerprint();
            if (fingerprint == null || fingerprint.isBlank()) {
                fingerprint = NewsStorySupport.fingerprint(ticker, source, headline);
            }
            if (!seen.add(fingerprint)) {
                continue;
            }
            lines.add(source + " | " + headline);
        }
        return lines;
    }

    public GeminiNewsPersistResult analyzeAndStore(List<String> tickers) {
        List<String> cleaned = cleanTickers(tickers);
        return analyzeInChunks(cleaned);
    }

    /**
     * Loads tickers from {@code stock} where {@code enabled = true}, then analyzes in
     * chunks sized by {@code schedule.chunk_size}.
     */
    public GeminiNewsPersistResult analyzeEnabledStocks() {
        List<String> enabledTickers = stocks.findEnabledTickers();
        if (enabledTickers.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No enabled stocks found. Set stock.enabled = true for at least one ticker.");
        }
        log.info("Analyzing {} enabled stock(s): {}", enabledTickers.size(), enabledTickers);
        return analyzeInChunks(enabledTickers);
    }

    private GeminiNewsPersistResult analyzeInChunks(List<String> tickers) {
        int chunkSize = readPositiveSetting(AnalyzeScheduleService.KEY_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
        int pauseSeconds = readNonNegativeSetting(
                AnalyzeScheduleService.KEY_CHUNK_PAUSE_SECONDS, DEFAULT_CHUNK_PAUSE_SECONDS);
        int totalChunks = (tickers.size() + chunkSize - 1) / chunkSize;
        log.info(
                "Gemini chunking: {} ticker(s) -> {} chunk(s) of up to {} (pause {}s)",
                tickers.size(),
                totalChunks,
                chunkSize,
                pauseSeconds);

        GeminiNewsPersistResult combined = null;
        for (int from = 0, chunkIndex = 1; from < tickers.size(); from += chunkSize, chunkIndex++) {
            List<String> chunk = tickers.subList(from, Math.min(from + chunkSize, tickers.size()));
            log.info("Gemini chunk {}/{}: {}", chunkIndex, totalChunks, chunk);
            GeminiNewsResponse response = requestAnalysis(chunk);
            List<String> chunkCopy = List.copyOf(chunk);
            GeminiNewsPersistResult part =
                    transactionTemplate.execute(status -> persist(response, chunkCopy));
            combined = GeminiNewsPersistResult.merge(combined, part);
            if (from + chunkSize < tickers.size() && pauseSeconds > 0) {
                sleepQuietly(pauseSeconds);
            }
        }
        return combined == null
                ? new GeminiNewsPersistResult(List.of(), 0, 0, 0, 0, null, null)
                : combined;
    }

    private int readPositiveSetting(String key, int fallback) {
        return Math.max(1, readIntSetting(key, fallback));
    }

    private int readNonNegativeSetting(String key, int fallback) {
        return Math.max(0, readIntSetting(key, fallback));
    }

    private int readIntSetting(String key, int fallback) {
        return appSettings.findById(key)
                .map(AppSetting::getValue)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (Exception exception) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private static void sleepQuietly(int seconds) {
        try {
            log.info("Pausing {}s before next Gemini chunk", seconds);
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Analyze interrupted");
        }
    }

    public GeminiNewsResponse requestAnalysis(List<String> tickers) {
        List<String> cleaned = cleanTickers(tickers);
        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API key is not configured. Set gemini.apiKey in db.properties.");
        }

        String prompt = buildPrompt(cleaned);
        String model = blankToDefault(geminiProperties.getModel(), "gemini-3.5-flash-lite");
        log.info("Requesting Gemini news analysis for {} using model {}", cleaned, model);
        log.debug("Gemini prompt exclusion section:\n{}", buildExclusionSection(cleaned));

        // Align with StockSugg: JSON mime + response schema, no Google Search tool.
        GenerateContentConfig config = GenerateContentConfig.builder()
                .candidateCount(1)
                .responseMimeType("application/json")
                .responseSchema(newsResponseSchema())
                .build();

        // Cap retries to original + 1 (SDK default can be higher and burns quota).
        try (Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .retryOptions(HttpRetryOptions.builder().attempts(2).build())
                        .build())
                .build()) {
            GenerateContentResponse response = client.models.generateContent(model, prompt, config);
            String text = response == null ? null : response.text();
            if (text == null || text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned an empty response");
            }
            return parseResponse(text);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Gemini request failed", exception);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Gemini request failed: " + exception.getMessage());
        }
    }

    private static Schema newsResponseSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "news_items", Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(tickerNewsSchema())
                                .build()))
                .required(List.of("news_items"))
                .build();
    }

    private static Schema tickerNewsSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "ticker", Schema.builder().type(Type.Known.STRING).build(),
                        "news", Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(newsEntrySchema())
                                .build(),
                        "normalized_index", normalizedIndexSchema(),
                        "time_horizon_outlook", timeHorizonOutlookSchema()))
                .required(List.of("ticker", "news", "normalized_index", "time_horizon_outlook"))
                .build();
    }

    private static Schema newsEntrySchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.ofEntries(
                        Map.entry("source", Schema.builder().type(Type.Known.STRING).build()),
                        Map.entry("media_tier", Schema.builder().type(Type.Known.STRING).build()),
                        Map.entry("release_time", Schema.builder().type(Type.Known.STRING).build()),
                        Map.entry("headline", Schema.builder().type(Type.Known.STRING).build()),
                        Map.entry("summary", Schema.builder().type(Type.Known.STRING).build()),
                        Map.entry("core_theme", Schema.builder().type(Type.Known.STRING).build()),
                        Map.entry("score", Schema.builder().type(Type.Known.INTEGER).build()),
                        Map.entry("justification", Schema.builder().type(Type.Known.STRING).build())))
                .required(List.of(
                        "source", "media_tier", "release_time", "headline",
                        "summary", "core_theme", "score", "justification"))
                .build();
    }

    private static Schema normalizedIndexSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "score", Schema.builder().type(Type.Known.NUMBER).build(),
                        "logic", Schema.builder().type(Type.Known.STRING).build()))
                .required(List.of("score", "logic"))
                .build();
    }

    private static Schema timeHorizonOutlookSchema() {
        Schema window = horizonWindowSchema();
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "immediate", window,
                        "10_days", window,
                        "30_days", window))
                .required(List.of("immediate", "10_days", "30_days"))
                .build();
    }

    private static Schema horizonWindowSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "score", Schema.builder().type(Type.Known.INTEGER).build(),
                        "description", Schema.builder().type(Type.Known.STRING).build()))
                .required(List.of("score", "description"))
                .build();
    }

    GeminiNewsResponse parseResponse(String rawText) {
        String json = extractJsonObject(rawText);
        try {
            GeminiNewsResponse parsed = objectMapper.readValue(json, GeminiNewsResponse.class);
            if (parsed == null || parsed.getNewsItems() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Gemini JSON did not contain news_items");
            }
            return parsed;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to parse Gemini JSON: {}", json);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Gemini returned JSON that could not be parsed");
        }
    }

    private GeminiNewsPersistResult persist(GeminiNewsResponse response, List<String> requestedTickers) {
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        Instant createdAt = Instant.now();
        List<Long> batchIds = new ArrayList<>();
        int itemCount = 0;
        int skippedDuplicateCount = 0;
        int clusteredCount = 0;
        int maxOverallHotness = 0;
        Double minAggregateSentiment = null;
        Double maxAggregateSentiment = null;
        Set<String> seenFingerprints = new HashSet<>();

        List<TickerNews> tickerNews = response.getNewsItems();
        if (tickerNews.isEmpty() && !requestedTickers.isEmpty()) {
            log.warn("Gemini returned no ticker blocks for {}", requestedTickers);
        }

        for (TickerNews block : tickerNews) {
            if (block == null || blankToNull(block.getTicker()) == null) {
                continue;
            }
            Stock stock = findOrCreateStock(block.getTicker());
            Integer tickerId = toTickerId(stock.getId());
            NewsBatch batch = new NewsBatch();
            batch.setTickerId(tickerId);
            batch.setCreateTime(now);
            applyNormalizedIndex(batch, block.getNormalizedIndex());
            applyOutlook(batch, block.getTimeHorizonOutlook());
            NewsBatch savedBatch = batches.save(batch);
            batchIds.add(savedBatch.getId());

            Instant clusterSince = createdAt.minus(NewsStorySupport.CLUSTER_LOOKBACK_HOURS, ChronoUnit.HOURS);
            List<NewsItem> recentForTicker = items.findRecentByTickerId(tickerId, clusterSince);

            List<NewsEntry> news = block.getNews() == null ? List.of() : block.getNews();
            for (NewsEntry entry : news) {
                if (entry == null) {
                    continue;
                }
                String source = clip(entry.getSource(), 32);
                String headline = entry.getHeadline();
                if (NewsStorySupport.normalizeHeadline(headline).isEmpty()) {
                    skippedDuplicateCount++;
                    continue;
                }
                String fingerprint = NewsStorySupport.fingerprint(stock.getTicker(), source, headline);

                // V1: hard dedupe — same ticker + source + normalized headline.
                if (!seenFingerprints.add(fingerprint)
                        || items.existsByHeadlineFingerprint(fingerprint)
                        || isHardDuplicateAgainstRecent(source, headline, recentForTicker)) {
                    skippedDuplicateCount++;
                    log.debug("Skipping duplicate news for {} / {}: {}", stock.getTicker(), source, headline);
                    continue;
                }

                NewsItem item = new NewsItem();
                item.setBatchId(savedBatch.getId());
                item.setTickerId(tickerId);
                item.setCreateTime(now);
                item.setCreatedAt(createdAt);
                item.setSource(source);
                item.setMediaTier(normalizeMediaTier(entry.getMediaTier()));
                item.setReleaseTime(parseReleaseTime(entry.getReleaseTime()));
                item.setHeadline(headline);
                item.setSummary(entry.getSummary());
                item.setCoreTheme(entry.getCoreTheme());
                item.setScore(entry.getScore());
                item.setJustification(entry.getJustification());
                item.setHeadlineFingerprint(fingerprint);
                item.setItemHotness(NewsStorySupport.tierHotnessWeight(item.getMediaTier()));

                // V2: soft cluster across sources for near-same stories.
                Long matchedClusterId = findMatchingStoryCluster(item, recentForTicker);
                if (matchedClusterId != null) {
                    item.setStoryClusterId(matchedClusterId);
                    clusteredCount++;
                } else {
                    NewsStoryCluster created = createStoryCluster(tickerId, item, createdAt);
                    item.setStoryClusterId(created.getId());
                }

                NewsItem saved = items.save(item);
                recentForTicker.add(0, saved);
                NewsStoryCluster refreshed = refreshClusterMetrics(saved.getStoryClusterId(), createdAt);
                if (refreshed != null) {
                    maxOverallHotness = Math.max(
                            maxOverallHotness,
                            refreshed.getOverallHotness() == null ? 0 : refreshed.getOverallHotness());
                    Double sentiment = refreshed.getAggregateSentiment();
                    if (sentiment != null) {
                        minAggregateSentiment = minAggregateSentiment == null
                                ? sentiment
                                : Math.min(minAggregateSentiment, sentiment);
                        maxAggregateSentiment = maxAggregateSentiment == null
                                ? sentiment
                                : Math.max(maxAggregateSentiment, sentiment);
                    }
                }
                itemCount++;
            }
        }

        log.info(
                "Stored {} batches, {} items (skippedDuplicates={}, clustered={}, maxOverallHotness={})",
                batchIds.size(),
                itemCount,
                skippedDuplicateCount,
                clusteredCount,
                maxOverallHotness);
        return new GeminiNewsPersistResult(
                batchIds,
                itemCount,
                skippedDuplicateCount,
                clusteredCount,
                maxOverallHotness,
                minAggregateSentiment,
                maxAggregateSentiment);
    }

    private NewsStoryCluster createStoryCluster(Integer tickerId, NewsItem item, Instant now) {
        NewsStoryCluster cluster = new NewsStoryCluster();
        cluster.setTickerId(tickerId);
        cluster.setCanonicalHeadline(item.getHeadline());
        cluster.setCoreTheme(item.getCoreTheme());
        cluster.setOverallHotness(item.getItemHotness() == null ? 1 : item.getItemHotness());
        cluster.setAggregateSentiment(item.getScore() == null ? null : item.getScore().doubleValue());
        cluster.setSourceCount(1);
        cluster.setFirstSeenAt(now);
        cluster.setLastSeenAt(now);
        cluster.setCreatedAt(now);
        return storyClusters.save(cluster);
    }

    private static boolean isHardDuplicateAgainstRecent(
            String source, String headline, List<NewsItem> recentForTicker) {
        String normalizedSource = NewsStorySupport.normalizeSource(source);
        String normalizedHeadline = NewsStorySupport.normalizeHeadline(headline);
        for (NewsItem existing : recentForTicker) {
            if (!normalizedSource.equals(NewsStorySupport.normalizeSource(existing.getSource()))) {
                continue;
            }
            if (normalizedHeadline.equals(NewsStorySupport.normalizeHeadline(existing.getHeadline()))) {
                return true;
            }
        }
        return false;
    }

    private Long findMatchingStoryCluster(NewsItem candidate, List<NewsItem> recentForTicker) {
        String candidateSource = NewsStorySupport.normalizeSource(candidate.getSource());
        for (NewsItem existing : recentForTicker) {
            if (existing.getHeadline() == null || existing.getStoryClusterId() == null) {
                continue;
            }
            String existingSource = NewsStorySupport.normalizeSource(existing.getSource());
            // Same source is handled by V1 fingerprint; clustering is for cross-source confirmation.
            if (!candidateSource.isEmpty() && candidateSource.equals(existingSource)) {
                continue;
            }
            double similarity = NewsStorySupport.jaccardSimilarity(candidate.getHeadline(), existing.getHeadline());
            if (similarity >= NewsStorySupport.STORY_SIMILARITY_THRESHOLD) {
                return existing.getStoryClusterId();
            }
        }
        return null;
    }

    private NewsStoryCluster refreshClusterMetrics(Long storyClusterId, Instant now) {
        if (storyClusterId == null) {
            return null;
        }
        NewsStoryCluster cluster = storyClusters.findById(storyClusterId).orElse(null);
        if (cluster == null) {
            return null;
        }
        List<NewsItem> clusterItems = items.findByStoryClusterId(storyClusterId);
        if (clusterItems.isEmpty()) {
            return cluster;
        }

        int overallHotness = NewsStorySupport.computeOverallHotness(clusterItems);
        Double aggregateSentiment = NewsStorySupport.computeAggregateSentiment(clusterItems);
        int sourceCount = NewsStorySupport.latestPerSource(clusterItems).size();
        NewsItem canonical = NewsStorySupport.pickCanonicalItem(clusterItems);

        cluster.setOverallHotness(overallHotness);
        cluster.setAggregateSentiment(aggregateSentiment);
        cluster.setSourceCount(Math.max(sourceCount, 1));
        cluster.setLastSeenAt(now);
        if (canonical != null) {
            cluster.setCanonicalHeadline(canonical.getHeadline());
            if (canonical.getCoreTheme() != null && !canonical.getCoreTheme().isBlank()) {
                cluster.setCoreTheme(canonical.getCoreTheme());
            }
        }
        if (cluster.getFirstSeenAt() == null) {
            cluster.setFirstSeenAt(now);
        }
        return storyClusters.save(cluster);
    }

    private Stock findOrCreateStock(String rawTicker) {
        String ticker = clip(rawTicker.trim().toUpperCase(Locale.ROOT), 12);
        return stocks.findFirstByTickerIgnoreCase(ticker).orElseGet(() -> {
            Stock created = new Stock();
            created.setTicker(ticker);
            created.setName(ticker);
            created.setEnabled(true);
            return stocks.save(created);
        });
    }

    private static void applyNormalizedIndex(NewsBatch batch, NormalizedIndex index) {
        if (index == null) {
            batch.setNormalizedScore(0.0);
            return;
        }
        batch.setNormalizedScore(index.getScore() == null ? 0.0 : index.getScore());
        batch.setNormalizedLogic(index.getLogic());
    }

    private static void applyOutlook(NewsBatch batch, TimeHorizonOutlook outlook) {
        if (outlook == null) {
            return;
        }
        HorizonWindow immediate = outlook.getImmediate();
        if (immediate != null) {
            batch.setOutlook0Score(immediate.getScore());
            batch.setOutlook0Descr(immediate.getDescription());
        }
        HorizonWindow tenDays = outlook.getTenDays();
        if (tenDays != null) {
            batch.setOutlook10Score(tenDays.getScore());
            batch.setOutlook10Descr(tenDays.getDescription());
        }
        HorizonWindow thirtyDays = outlook.getThirtyDays();
        if (thirtyDays != null) {
            batch.setOutlook30Score(thirtyDays.getScore());
            batch.setOutlook30Descr(thirtyDays.getDescription());
        }
    }

    private String resolveApiKey() {
        String configured = geminiProperties.getApiKey();
        if (configured != null && !configured.isBlank() && !"change-me".equalsIgnoreCase(configured)) {
            return configured.trim();
        }
        String geminiEnv = System.getenv("GEMINI_API_KEY");
        if (geminiEnv != null && !geminiEnv.isBlank()) {
            return geminiEnv.trim();
        }
        String googleEnv = System.getenv("GOOGLE_API_KEY");
        return googleEnv == null ? "" : googleEnv.trim();
    }

    private static List<String> cleanTickers(List<String> tickers) {
        if (tickers == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one ticker is required");
        }
        List<String> cleaned = tickers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (cleaned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one ticker is required");
        }
        return cleaned;
    }

    private static String formatTickers(List<String> tickers) {
        return String.join(", ", tickers);
    }

    static String extractJsonObject(String rawText) {
        String trimmed = rawText.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    static String normalizeMediaTier(String rawTier) {
        if (rawTier == null || rawTier.isBlank()) {
            return null;
        }
        String lowered = rawTier.toLowerCase(Locale.ROOT);
        if (lowered.contains("1") || lowered.contains("institutional")) {
            return "Tier 1";
        }
        if (lowered.contains("3") || lowered.contains("specialist")) {
            return "Tier 3";
        }
        if (lowered.contains("2") || lowered.contains("mainstream")) {
            return "Tier 2";
        }
        return clip(rawTier.trim(), 10);
    }

    static LocalTime parseReleaseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return LocalDateTime.parse(value, RELEASE_DATE_TIME).toLocalTime();
        } catch (DateTimeParseException ignored) {
            // try ISO date-time next
        }
        try {
            return LocalDateTime.parse(value).toLocalTime();
        } catch (DateTimeParseException ignored) {
            // try time-only next
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static Integer toTickerId(Long stockId) {
        if (stockId == null) {
            return null;
        }
        if (stockId > Integer.MAX_VALUE || stockId < Integer.MIN_VALUE) {
            throw new IllegalStateException("stock.id is too large for news_batch.ticker_id");
        }
        return stockId.intValue();
    }

    private static String clip(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
