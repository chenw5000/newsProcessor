-- Promote story clusters to a first-class table; split item vs overall hotness.

ALTER TABLE news_item RENAME COLUMN hotness TO item_hotness;

CREATE TABLE IF NOT EXISTS news_story_cluster (
    id bigint PRIMARY KEY DEFAULT nextval('news_story_cluster_id_seq'),
    ticker_id integer,
    canonical_headline text,
    core_theme text,
    overall_hotness integer NOT NULL DEFAULT 1,
    aggregate_sentiment double precision,
    source_count integer NOT NULL DEFAULT 1,
    first_seen_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);

ALTER SEQUENCE news_story_cluster_id_seq OWNED BY news_story_cluster.id;

-- Backfill cluster rows from existing item groupings (ids kept stable).
INSERT INTO news_story_cluster (
    id,
    ticker_id,
    canonical_headline,
    core_theme,
    overall_hotness,
    source_count,
    first_seen_at,
    last_seen_at,
    created_at
)
SELECT
    ni.story_cluster_id,
    MIN(ni.ticker_id),
    (array_agg(ni.headline ORDER BY ni.created_at ASC NULLS LAST))[1],
    (array_agg(ni.core_theme ORDER BY ni.created_at ASC NULLS LAST))[1],
    GREATEST(COALESCE(MAX(ni.item_hotness), 1), 1),
    GREATEST(COUNT(DISTINCT lower(trim(COALESCE(ni.source, '')))), 1),
    COALESCE(MIN(ni.created_at), now()),
    COALESCE(MAX(ni.created_at), now()),
    COALESCE(MIN(ni.created_at), now())
FROM news_item ni
WHERE ni.story_cluster_id IS NOT NULL
GROUP BY ni.story_cluster_id
ON CONFLICT (id) DO NOTHING;

SELECT setval(
    'news_story_cluster_id_seq',
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM news_story_cluster), 1),
    true
);

-- Item hotness becomes per-article tier weight (3 / 2 / 1).
UPDATE news_item
SET item_hotness = CASE
    WHEN media_tier ILIKE '%1%' THEN 3
    WHEN media_tier ILIKE '%3%' THEN 1
    WHEN media_tier ILIKE '%2%' THEN 2
    ELSE 1
END;

-- Recompute overall_hotness + aggregate_sentiment (latest item per source).
WITH ranked AS (
    SELECT
        ni.story_cluster_id,
        lower(trim(COALESCE(ni.source, ''))) AS source_key,
        ni.media_tier,
        COALESCE(ni.score, 0) AS score,
        ni.created_at,
        ROW_NUMBER() OVER (
            PARTITION BY ni.story_cluster_id, lower(trim(COALESCE(ni.source, '')))
            ORDER BY ni.created_at DESC NULLS LAST, ni.id DESC
        ) AS rn
    FROM news_item ni
    WHERE ni.story_cluster_id IS NOT NULL
),
picked AS (
    SELECT
        story_cluster_id,
        source_key,
        CASE
            WHEN media_tier ILIKE '%1%' THEN 3
            WHEN media_tier ILIKE '%3%' THEN 1
            WHEN media_tier ILIKE '%2%' THEN 2
            ELSE 1
        END AS hot_weight,
        CASE
            WHEN media_tier ILIKE '%1%' THEN 1.5
            WHEN media_tier ILIKE '%3%' THEN 1.2
            WHEN media_tier ILIKE '%2%' THEN 1.0
            ELSE 1.0
        END AS sentiment_weight,
        score
    FROM ranked
    WHERE rn = 1
),
agg AS (
    SELECT
        story_cluster_id,
        GREATEST(COALESCE(SUM(hot_weight), 1), 1)::integer AS overall_hotness,
        CASE
            WHEN SUM(sentiment_weight) = 0 THEN NULL
            ELSE SUM(sentiment_weight * score) / SUM(sentiment_weight)
        END AS aggregate_sentiment,
        GREATEST(COUNT(*)::integer, 1) AS source_count
    FROM picked
    GROUP BY story_cluster_id
)
UPDATE news_story_cluster c
SET
    overall_hotness = a.overall_hotness,
    aggregate_sentiment = a.aggregate_sentiment,
    source_count = a.source_count
FROM agg a
WHERE c.id = a.story_cluster_id;

-- Drop dangling item references before FK (should be none after backfill).
UPDATE news_item ni
SET story_cluster_id = NULL
WHERE story_cluster_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM news_story_cluster c WHERE c.id = ni.story_cluster_id
  );

ALTER TABLE news_item
    DROP CONSTRAINT IF EXISTS news_item_story_cluster_fk;

ALTER TABLE news_item
    ADD CONSTRAINT news_item_story_cluster_fk
    FOREIGN KEY (story_cluster_id) REFERENCES news_story_cluster (id);

CREATE INDEX IF NOT EXISTS news_story_cluster_ticker_hotness_idx
    ON news_story_cluster (ticker_id, overall_hotness DESC);

CREATE INDEX IF NOT EXISTS news_story_cluster_last_seen_idx
    ON news_story_cluster (last_seen_at DESC);
