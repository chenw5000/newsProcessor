package com.newsprocessor.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiNewsResponse {

    @JsonProperty("news_items")
    private List<TickerNews> newsItems;

    public List<TickerNews> getNewsItems() {
        return newsItems;
    }

    public void setNewsItems(List<TickerNews> newsItems) {
        this.newsItems = newsItems;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TickerNews {

        private String ticker;
        private List<NewsEntry> news;

        @JsonProperty("normalized_index")
        private NormalizedIndex normalizedIndex;

        @JsonProperty("time_horizon_outlook")
        private TimeHorizonOutlook timeHorizonOutlook;

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

        public List<NewsEntry> getNews() {
            return news;
        }

        public void setNews(List<NewsEntry> news) {
            this.news = news;
        }

        public NormalizedIndex getNormalizedIndex() {
            return normalizedIndex;
        }

        public void setNormalizedIndex(NormalizedIndex normalizedIndex) {
            this.normalizedIndex = normalizedIndex;
        }

        public TimeHorizonOutlook getTimeHorizonOutlook() {
            return timeHorizonOutlook;
        }

        public void setTimeHorizonOutlook(TimeHorizonOutlook timeHorizonOutlook) {
            this.timeHorizonOutlook = timeHorizonOutlook;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsEntry {

        private String source;

        @JsonProperty("media_tier")
        private String mediaTier;

        @JsonProperty("release_time")
        private String releaseTime;

        private String headline;
        private String summary;

        @JsonProperty("core_theme")
        private String coreTheme;

        private Integer score;
        private String justification;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getMediaTier() {
            return mediaTier;
        }

        public void setMediaTier(String mediaTier) {
            this.mediaTier = mediaTier;
        }

        public String getReleaseTime() {
            return releaseTime;
        }

        public void setReleaseTime(String releaseTime) {
            this.releaseTime = releaseTime;
        }

        public String getHeadline() {
            return headline;
        }

        public void setHeadline(String headline) {
            this.headline = headline;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getCoreTheme() {
            return coreTheme;
        }

        public void setCoreTheme(String coreTheme) {
            this.coreTheme = coreTheme;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getJustification() {
            return justification;
        }

        public void setJustification(String justification) {
            this.justification = justification;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NormalizedIndex {

        private Double score;
        private String logic;

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getLogic() {
            return logic;
        }

        public void setLogic(String logic) {
            this.logic = logic;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeHorizonOutlook {

        private HorizonWindow immediate;

        @JsonProperty("10_days")
        private HorizonWindow tenDays;

        @JsonProperty("30_days")
        private HorizonWindow thirtyDays;

        public HorizonWindow getImmediate() {
            return immediate;
        }

        public void setImmediate(HorizonWindow immediate) {
            this.immediate = immediate;
        }

        public HorizonWindow getTenDays() {
            return tenDays;
        }

        public void setTenDays(HorizonWindow tenDays) {
            this.tenDays = tenDays;
        }

        public HorizonWindow getThirtyDays() {
            return thirtyDays;
        }

        public void setThirtyDays(HorizonWindow thirtyDays) {
            this.thirtyDays = thirtyDays;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HorizonWindow {

        private Integer score;
        private String description;

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
