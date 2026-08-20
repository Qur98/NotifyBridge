-- NotifyBridge V5.0 Complete - Fresh installation schema
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 device_id VARCHAR(100) NOT NULL,
 app_package VARCHAR(255) NOT NULL,
 app_name VARCHAR(255) DEFAULT NULL,
 title VARCHAR(500) DEFAULT NULL,
 message TEXT,
 notification_key VARCHAR(255) NOT NULL,
 sent_at VARCHAR(64) DEFAULT NULL,
 received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 is_read TINYINT(1) NOT NULL DEFAULT 0,
 UNIQUE KEY uniq_key(device_id,notification_key),
 KEY idx_received(received_at), KEY idx_app(app_package), KEY idx_device(device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS devices (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 device_id VARCHAR(100) NOT NULL UNIQUE,
 device_name VARCHAR(190) DEFAULT NULL,
 first_seen DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_seen DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_ip VARCHAR(64) DEFAULT NULL,
 notification_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
 status ENUM('online','offline') NOT NULL DEFAULT 'online',
 KEY idx_last_seen(last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS apps (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 app_package VARCHAR(255) NOT NULL UNIQUE,
 app_name VARCHAR(255) DEFAULT NULL,
 first_seen DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_seen DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 notification_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
 is_active TINYINT(1) NOT NULL DEFAULT 1,
 KEY idx_last_seen(last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_logs (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 event_type VARCHAR(80) NOT NULL,
 device_id VARCHAR(100) DEFAULT NULL,
 app_package VARCHAR(255) DEFAULT NULL,
 details VARCHAR(1000) DEFAULT NULL,
 ip_address VARCHAR(64) DEFAULT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_created(created_at), KEY idx_event(event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS settings (
 setting_key VARCHAR(100) PRIMARY KEY,
 setting_value TEXT DEFAULT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO settings(setting_key,setting_value) VALUES
 ('site_name','NotifyBridge'),('version','5.0'),('developer','محمد القرعان'),('facebook','https://www.facebook.com/elqdes'),('instagram','https://www.instagram.com/elqdes/')
ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value);


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
