-- Watch-list flag: only enabled tickers are included in analyze-enabled runs.
ALTER TABLE stock
    ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS stock_enabled_ticker_idx
    ON stock (enabled, ticker);
