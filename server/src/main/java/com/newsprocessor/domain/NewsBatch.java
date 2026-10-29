package com.newsprocessor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "news_batch")
public class NewsBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker_id")
    private Integer tickerId;

    @Column(name = "create_time")
    private LocalTime createTime;

    @Column(name = "normalized_score")
    private Double normalizedScore;

    @Column(name = "normalized_logic")
    private String normalizedLogic;

    @Column(name = "outlook0_score")
    private Integer outlook0Score;

    @Column(name = "outlook0_descr")
    private String outlook0Descr;

    @Column(name = "outlook10_score")
    private Integer outlook10Score;

    @Column(name = "outlook10_descr")
    private String outlook10Descr;

    @Column(name = "outlook30_score")
    private Integer outlook30Score;

    @Column(name = "outlook30_descr")
    private String outlook30Descr;

    public NewsBatch() {
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

    public LocalTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalTime createTime) {
        this.createTime = createTime;
    }

    public Double getNormalizedScore() {
        return normalizedScore;
    }

    public void setNormalizedScore(Double normalizedScore) {
        this.normalizedScore = normalizedScore;
    }

    public String getNormalizedLogic() {
        return normalizedLogic;
    }

    public void setNormalizedLogic(String normalizedLogic) {
        this.normalizedLogic = normalizedLogic;
    }

    public Integer getOutlook0Score() {
        return outlook0Score;
    }

    public void setOutlook0Score(Integer outlook0Score) {
        this.outlook0Score = outlook0Score;
    }

    public String getOutlook0Descr() {
        return outlook0Descr;
    }

    public void setOutlook0Descr(String outlook0Descr) {
        this.outlook0Descr = outlook0Descr;
    }

    public Integer getOutlook10Score() {
        return outlook10Score;
    }

    public void setOutlook10Score(Integer outlook10Score) {
        this.outlook10Score = outlook10Score;
    }

    public String getOutlook10Descr() {
        return outlook10Descr;
    }

    public void setOutlook10Descr(String outlook10Descr) {
        this.outlook10Descr = outlook10Descr;
    }

    public Integer getOutlook30Score() {
        return outlook30Score;
    }

    public void setOutlook30Score(Integer outlook30Score) {
        this.outlook30Score = outlook30Score;
    }

    public String getOutlook30Descr() {
        return outlook30Descr;
    }

    public void setOutlook30Descr(String outlook30Descr) {
        this.outlook30Descr = outlook30Descr;
    }
}
