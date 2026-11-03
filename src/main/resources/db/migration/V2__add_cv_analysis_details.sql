-- Migration V2: Add detailed CV analysis fields (score, summary, strengths, improvements)
ALTER TABLE cv_analyses
    ADD COLUMN IF NOT EXISTS cv_score INT DEFAULT 75,
    ADD COLUMN IF NOT EXISTS summary TEXT,
    ADD COLUMN IF NOT EXISTS strengths_json TEXT,
    ADD COLUMN IF NOT EXISTS improvements_json TEXT;
