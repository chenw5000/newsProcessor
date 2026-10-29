package com.newsprocessor.web.dto;

public class ScheduleStatusResponse {

    private boolean enabled;
    private String timezone;
    private String windowStart;
    private String windowEnd;
    private int intervalMinutes;
    private int chunkSize;
    private int chunkPauseSeconds;
    private String lastRunAt;
    private boolean runInProgress;
    private String runStartedAt;
    private String lastRunStatus;
    private String lastError;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
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

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkPauseSeconds() {
        return chunkPauseSeconds;
    }

    public void setChunkPauseSeconds(int chunkPauseSeconds) {
        this.chunkPauseSeconds = chunkPauseSeconds;
    }

    public String getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(String lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public boolean isRunInProgress() {
        return runInProgress;
    }

    public void setRunInProgress(boolean runInProgress) {
        this.runInProgress = runInProgress;
    }

    public String getRunStartedAt() {
        return runStartedAt;
    }

    public void setRunStartedAt(String runStartedAt) {
        this.runStartedAt = runStartedAt;
    }

    public String getLastRunStatus() {
        return lastRunStatus;
    }

    public void setLastRunStatus(String lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
