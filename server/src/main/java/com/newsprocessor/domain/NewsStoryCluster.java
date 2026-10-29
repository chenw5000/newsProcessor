package com.newsprocessor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "news_story_cluster")
public class NewsStoryCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "news_story_cluster_id_gen")
    @SequenceGenerator(
            name = "news_story_cluster_id_gen",
            sequenceName = "news_story_cluster_id_seq",
            allocationSize = 1)
    private Long id;

    @Column(name = "ticker_id")
    private Integer tickerId;

    @Column(name = "canonical_headline")
    private String canonicalHeadline;

    @Column(name = "core_theme")
    private String coreTheme;

    @Column(name = "overall_hotness", nullable = false)
    private Integer overallHotness = 1;

    @Column(name = "aggregate_sentiment")
    private Double aggregateSentiment;

    @Column(name = "source_count", nullable = false)
    private Integer sourceCount = 1;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NewsStoryCluster() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTickerId() {
        return tickerId;
    }

    public void setTickerId(Integer tickerId) {
        this.tickerId = tickerId;
    }

    public String getCanonicalHeadline() {
        return canonicalHeadline;
    }

    public void setCanonicalHeadline(String canonicalHeadline) {
        this.canonicalHeadline = canonicalHeadline;
    }

    public String getCoreTheme() {
        return coreTheme;
    }

    public void setCoreTheme(String coreTheme) {
        this.coreTheme = coreTheme;
    }

    public Integer getOverallHotness() {
        return overallHotness;
    }

    public void setOverallHotness(Integer overallHotness) {
        this.overallHotness = overallHotness;
    }

    public Double getAggregateSentiment() {
        return aggregateSentiment;
    }

    public void setAggregateSentiment(Double aggregateSentiment) {
        this.aggregateSentiment = aggregateSentiment;
    }

    public Integer getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(Integer sourceCount) {
        this.sourceCount = sourceCount;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
