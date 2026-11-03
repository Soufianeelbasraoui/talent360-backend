CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_refresh_token_hash (token_hash),
    INDEX idx_refresh_token_family (family_id),
    INDEX idx_refresh_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
