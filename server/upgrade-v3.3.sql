CREATE TABLE IF NOT EXISTS api_keys (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 key_name VARCHAR(120) NOT NULL,
 api_key VARCHAR(128) NOT NULL UNIQUE,
 is_active TINYINT(1) NOT NULL DEFAULT 1,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_used_at DATETIME DEFAULT NULL,
 use_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
 KEY idx_active(is_active), KEY idx_last_used(last_used_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO settings(setting_key,setting_value) VALUES ('version','3.3') ON DUPLICATE KEY UPDATE setting_value='3.3';
