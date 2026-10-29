package com.newsprocessor.service.schedule;

import com.newsprocessor.domain.AppSetting;
import com.newsprocessor.repo.AppSettingRepository;
import com.newsprocessor.service.GeminiNewsPersistResult;
import com.newsprocessor.service.GeminiNewsService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalyzeScheduleService {

    public static final String KEY_ENABLED = "schedule.enabled";
    public static final String KEY_TIMEZONE = "schedule.timezone";
    public static final String KEY_WINDOW_START = "schedule.window_start";
    public static final String KEY_WINDOW_END = "schedule.window_end";
    public static final String KEY_INTERVAL_MINUTES = "schedule.interval_minutes";
    public static final String KEY_CHUNK_SIZE = "schedule.chunk_size";
    public static final String KEY_CHUNK_PAUSE_SECONDS = "schedule.chunk_pause_seconds";
    public static final String KEY_LAST_RUN_AT = "schedule.last_run_at";
    public static final String KEY_RUN_IN_PROGRESS = "schedule.run_in_progress";
    public static final String KEY_RUN_STARTED_AT = "schedule.run_started_at";
    public static final String KEY_LAST_RUN_STATUS = "schedule.last_run_status";
    public static final String KEY_LAST_ERROR = "schedule.last_error";

    static final Duration STALE_LOCK_AFTER = Duration.ofMinutes(30);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Logger log = LoggerFactory.getLogger(AnalyzeScheduleService.class);

    private static final List<String> ALL_KEYS = List.of(
            KEY_ENABLED,
            KEY_TIMEZONE,
            KEY_WINDOW_START,
            KEY_WINDOW_END,
            KEY_INTERVAL_MINUTES,
            KEY_CHUNK_SIZE,
            KEY_CHUNK_PAUSE_SECONDS,
            KEY_LAST_RUN_AT,
            KEY_RUN_IN_PROGRESS,
            KEY_RUN_STARTED_AT,
            KEY_LAST_RUN_STATUS,
            KEY_LAST_ERROR);

    private final AppSettingRepository settings;
    private final GeminiNewsService geminiNewsService;
    private final TransactionTemplate tx;

    public AnalyzeScheduleService(
            AppSettingRepository settings,
            GeminiNewsService geminiNewsService,
            PlatformTransactionManager transactionManager) {
        this.settings = settings;
        this.geminiNewsService = geminiNewsService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public ScheduleConfig getConfig() {
        return tx.execute(status -> readConfig());
    }

    public ScheduleConfig updateConfig(
            Boolean enabled,
            String timezone,
            String windowStart,
            String windowEnd,
            Integer intervalMinutes,
            Integer chunkSize,
            Integer chunkPauseSeconds) {
        return tx.execute(status -> {
            if (enabled != null) {
                put(KEY_ENABLED, Boolean.toString(enabled));
            }
            if (timezone != null && !timezone.isBlank()) {
                ZoneId.of(timezone.trim());
                put(KEY_TIMEZONE, timezone.trim());
            }
            if (windowStart != null && !windowStart.isBlank()) {
                LocalTime.parse(windowStart.trim(), TIME);
                put(KEY_WINDOW_START, windowStart.trim());
            }
            if (windowEnd != null && !windowEnd.isBlank()) {
                LocalTime.parse(windowEnd.trim(), TIME);
                put(KEY_WINDOW_END, windowEnd.trim());
            }
            if (intervalMinutes != null) {
                if (intervalMinutes < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "intervalMinutes must be >= 1");
                }
                put(KEY_INTERVAL_MINUTES, Integer.toString(intervalMinutes));
            }
            if (chunkSize != null) {
                if (chunkSize < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chunkSize must be >= 1");
                }
                put(KEY_CHUNK_SIZE, Integer.toString(chunkSize));
            }
            if (chunkPauseSeconds != null) {
                if (chunkPauseSeconds < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chunkPauseSeconds must be >= 0");
                }
                put(KEY_CHUNK_PAUSE_SECONDS, Integer.toString(chunkPauseSeconds));
            }
            return readConfig();
        });
    }

    /** Scheduler tick: may start a run based on DB settings. */
    public boolean tick() {
        ScheduleConfig config = getConfig();
        clearStaleLockIfNeeded(config);
        config = getConfig();

        if (!config.enabled()) {
            return false;
        }
        if (config.runInProgress()) {
            log.debug("Schedule tick skipped: run already in progress");
            return false;
        }
        if (!withinWindow(config)) {
            return false;
        }
        if (!intervalElapsed(config)) {
            return false;
        }
        if (!tryAcquireLock()) {
            log.debug("Schedule tick skipped: could not acquire run lock");
            return false;
        }
        executeLockedRun("SCHEDULED");
        return true;
    }

    /** After JVM restart, no prior run can still be active. */
    public void clearLockOnStartup() {
        tx.executeWithoutResult(status -> {
            put(KEY_RUN_IN_PROGRESS, "false");
            put(KEY_RUN_STARTED_AT, null);
        });
    }

    /** Manual /analyze-enabled with the same overlap lock. */
    public GeminiNewsPersistResult runManualAnalyzeEnabled() {
        clearStaleLockIfNeeded(getConfig());
        if (!tryAcquireLock()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An analyze run is already in progress");
        }
        return executeLockedRun("MANUAL");
    }

    private GeminiNewsPersistResult executeLockedRun(String trigger) {
        Instant started = Instant.now();
        tx.executeWithoutResult(status -> put(KEY_RUN_STARTED_AT, started.toString()));
        try {
            log.info("Starting {} analyze-enabled run", trigger);
            GeminiNewsPersistResult result = geminiNewsService.analyzeEnabledStocks();
            Instant finished = Instant.now();
            tx.executeWithoutResult(status -> {
                put(KEY_LAST_RUN_AT, finished.toString());
                put(KEY_LAST_RUN_STATUS, "SUCCESS");
                put(KEY_LAST_ERROR, null);
            });
            log.info(
                    "{} analyze-enabled finished: batches={}, items={}",
                    trigger,
                    result.getBatchIds().size(),
                    result.getItemCount());
            return result;
        } catch (RuntimeException exception) {
            Instant finished = Instant.now();
            tx.executeWithoutResult(status -> {
                put(KEY_LAST_RUN_AT, finished.toString());
                put(KEY_LAST_RUN_STATUS, "FAILED");
                put(KEY_LAST_ERROR, clip(exception.getMessage(), 500));
            });
            log.error("{} analyze-enabled failed", trigger, exception);
            throw exception;
        } finally {
            tx.executeWithoutResult(status -> {
                put(KEY_RUN_IN_PROGRESS, "false");
                put(KEY_RUN_STARTED_AT, null);
            });
        }
    }

    private boolean tryAcquireLock() {
        Boolean acquired = tx.execute(status -> settings.tryAcquireRunLock() > 0);
        return Boolean.TRUE.equals(acquired);
    }

    private void clearStaleLockIfNeeded(ScheduleConfig config) {
        if (!config.runInProgress()) {
            return;
        }
        Instant started = config.runStartedAt();
        if (started == null || Duration.between(started, Instant.now()).compareTo(STALE_LOCK_AFTER) > 0) {
            log.warn("Clearing stale schedule.run_in_progress lock (startedAt={})", started);
            tx.executeWithoutResult(status -> {
                put(KEY_RUN_IN_PROGRESS, "false");
                put(KEY_RUN_STARTED_AT, null);
            });
        }
    }

    private boolean withinWindow(ScheduleConfig config) {
        LocalTime local = ZonedDateTime.now(config.timezone()).toLocalTime();
        LocalTime start = config.windowStart();
        LocalTime end = config.windowEnd();
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !local.isBefore(start) && !local.isAfter(end);
        }
        // Overnight window support (e.g. 22:00–06:00).
        return !local.isBefore(start) || !local.isAfter(end);
    }

    private boolean intervalElapsed(ScheduleConfig config) {
        Instant last = config.lastRunAt();
        if (last == null) {
            return true;
        }
        return Duration.between(last, Instant.now()).toMinutes() >= config.intervalMinutes();
    }

    private ScheduleConfig readConfig() {
        Map<String, String> map = loadMap();
        return new ScheduleConfig(
                parseBoolean(map.get(KEY_ENABLED), false),
                parseZone(map.get(KEY_TIMEZONE), ZoneId.of("America/Los_Angeles")),
                parseTime(map.get(KEY_WINDOW_START), LocalTime.of(5, 30)),
                parseTime(map.get(KEY_WINDOW_END), LocalTime.of(17, 30)),
                parseInt(map.get(KEY_INTERVAL_MINUTES), 15),
                Math.max(1, parseInt(map.get(KEY_CHUNK_SIZE), 8)),
                Math.max(0, parseInt(map.get(KEY_CHUNK_PAUSE_SECONDS), 30)),
                parseInstant(map.get(KEY_LAST_RUN_AT)),
                parseBoolean(map.get(KEY_RUN_IN_PROGRESS), false),
                parseInstant(map.get(KEY_RUN_STARTED_AT)),
                blankToNull(map.get(KEY_LAST_RUN_STATUS)),
                blankToNull(map.get(KEY_LAST_ERROR)));
    }

    private Map<String, String> loadMap() {
        Map<String, String> map = new HashMap<>();
        for (AppSetting setting : settings.findByKeyIn(ALL_KEYS)) {
            map.put(setting.getKey(), setting.getValue());
        }
        return map;
    }

    private void put(String key, String value) {
        Optional<AppSetting> existing = settings.findById(key);
        AppSetting setting = existing.orElseGet(() -> new AppSetting(key, value));
        setting.setValue(value);
        setting.setUpdatedAt(Instant.now());
        settings.save(setting);
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static LocalTime parseTime(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(raw.trim(), TIME);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private static ZoneId parseZone(String raw, ZoneId fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }
}
