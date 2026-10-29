package com.newsprocessor.web.dto;

public class ScheduleUpdateRequest {

    private Boolean enabled;
    private String timezone;
    private String windowStart;
    private String windowEnd;
    private Integer intervalMinutes;
    private Integer chunkSize;
    private Integer chunkPauseSeconds;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(String windowStart) {
        this.windowStart = windowStart;
    }

    public String getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(String windowEnd) {
        this.windowEnd = windowEnd;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkPauseSeconds() {
        return chunkPauseSeconds;
    }

    public void setChunkPauseSeconds(Integer chunkPauseSeconds) {
        this.chunkPauseSeconds = chunkPauseSeconds;
    }
}
