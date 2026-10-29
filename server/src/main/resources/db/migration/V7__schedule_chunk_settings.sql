-- Gemini request chunking for large enabled-stock lists.
INSERT INTO app_setting (key, value) VALUES
    ('schedule.chunk_size', '8'),
    ('schedule.chunk_pause_seconds', '30')
ON CONFLICT (key) DO NOTHING;
