-- V1/V2: hard-dedupe fingerprint, story clusters, and hotness.
CREATE SEQUENCE IF NOT EXISTS news_story_cluster_id_seq;

ALTER TABLE news_item
    ADD COLUMN IF NOT EXISTS ticker_id integer,
    ADD COLUMN IF NOT EXISTS headline_fingerprint character varying(64),
    ADD COLUMN IF NOT EXISTS story_cluster_id bigint,
    ADD COLUMN IF NOT EXISTS hotness integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();

UPDATE news_item AS ni
SET ticker_id = nb.ticker_id
FROM news_batch AS nb
WHERE ni.batch_id = nb.id
  AND ni.ticker_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS news_item_fingerprint_uidx
    ON news_item (headline_fingerprint)
    WHERE headline_fingerprint IS NOT NULL;

CREATE INDEX IF NOT EXISTS news_item_ticker_created_idx
    ON news_item (ticker_id, created_at DESC);

CREATE INDEX IF NOT EXISTS news_item_cluster_idx
    ON news_item (story_cluster_id);
