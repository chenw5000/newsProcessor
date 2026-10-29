package com.newsprocessor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "news_item")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "ticker_id")
    private Integer tickerId;

    @Column(name = "create_time")
    private LocalTime createTime;

    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "media_tier", length = 10)
    private String mediaTier;

    @Column(name = "release_time")
    private LocalTime releaseTime;

    @Column(name = "headline")
    private String headline;

    @Column(name = "summary")
    private String summary;

    @Column(name = "core_theme")
    private String coreTheme;

    @Column(name = "score")
    private Integer score;

    @Column(name = "justification")
    private String justification;

    @Column(name = "headline_fingerprint", length = 64)
    private String headlineFingerprint;

    @Column(name = "story_cluster_id")
    private Long storyClusterId;

    @Column(name = "item_hotness", nullable = false)
    private Integer itemHotness = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public NewsItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getTickerId() {
        return tickerId;
    }

    public void setTickerId(Integer tickerId) {
        this.tickerId = tickerId;
    }

    public LocalTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalTime createTime) {
        this.createTime = createTime;
    }

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

    public LocalTime getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(LocalTime releaseTime) {
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

    public String getHeadlineFingerprint() {
        return headlineFingerprint;
    }

    public void setHeadlineFingerprint(String headlineFingerprint) {
        this.headlineFingerprint = headlineFingerprint;
    }

    public Long getStoryClusterId() {
        return storyClusterId;
    }

    public void setStoryClusterId(Long storyClusterId) {
        this.storyClusterId = storyClusterId;
    }

    public Integer getItemHotness() {
        return itemHotness;
    }

    public void setItemHotness(Integer itemHotness) {
        this.itemHotness = itemHotness;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
