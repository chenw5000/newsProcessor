-- Key/value app settings used by the news analyze scheduler.
CREATE TABLE IF NOT EXISTS app_setting (
    key character varying(64) PRIMARY KEY,
    value text,
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO app_setting (key, value) VALUES
    ('schedule.enabled', 'false'),
    ('schedule.timezone', 'America/Los_Angeles'),
    ('schedule.window_start', '05:30'),
    ('schedule.window_end', '17:30'),
    ('schedule.interval_minutes', '15'),
    ('schedule.last_run_at', NULL),
    ('schedule.run_in_progress', 'false'),
    ('schedule.run_started_at', NULL),
    ('schedule.last_run_status', NULL),
    ('schedule.last_error', NULL)
ON CONFLICT (key) DO NOTHING;
