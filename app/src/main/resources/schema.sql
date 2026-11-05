CREATE SEQUENCE IF NOT EXISTS dashboard_number_seq
  START WITH 1
  INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS dashboard_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    number BIGINT DEFAULT NEXT VALUE FOR dashboard_number_seq NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS card_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS card_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    number BIGINT DEFAULT NEXT VALUE FOR card_number_seq NOT NULL,
    subscription_topic VARCHAR(255),
    subscription_qos VARCHAR(50),
    subscription_data_name VARCHAR(255),
    subscription_data_type VARCHAR(50),
    display_data_jsonpath VARCHAR(255),
    publication_topic VARCHAR(255),
    publication_qos VARCHAR(50),
    publication_retain BOOLEAN,
    publication_data VARCHAR(10000),
    publication_data_type VARCHAR(255),
    local_task_path VARCHAR(255),
    local_task_arguments VARCHAR(255),
    local_task_data_type VARCHAR(50),
    dashboard_id BIGINT NOT NULL,
    FOREIGN KEY (dashboard_id) REFERENCES dashboard_settings(id) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS startup_task_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS startup_task_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(255),
    arguments VARCHAR(255),
    number BIGINT DEFAULT NEXT VALUE FOR startup_task_number_seq NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS mqtt_settings_number_seq
  START WITH 1
  INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS mqtt_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    host VARCHAR(255),
    port VARCHAR(50), -- VARCHAR(50): сущность MqttSettingsEntity.port имеет тип String; INT ломает существующие БД и маппинг Hibernate
    mq2t_password VARCHAR(255),
    mq2t_username VARCHAR(255),
    client_id VARCHAR(255),
    has_username BOOLEAN,
    has_password BOOLEAN,
    will_qos VARCHAR(50),
    will_retain BOOLEAN,
    will_flag BOOLEAN,
    clean_session BOOLEAN,
    auto_connect BOOLEAN,
    will_topic VARCHAR(255),
    will_message VARCHAR(10000),
    reconnect BOOLEAN,
    number BIGINT DEFAULT NEXT VALUE FOR mqtt_settings_number_seq NOT NULL
);

CREATE TABLE IF NOT EXISTS pending_mqtt_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    packet_id INT NOT NULL DEFAULT 0, -- Синхронизировано с дефолтом миграции
    topic VARCHAR(255) NOT NULL,
    payload BLOB NOT NULL,
    qos INT NOT NULL,
    retain BOOLEAN,
    status VARCHAR(20) DEFAULT NULL, -- Синхронизировано с дефолтом миграции
    created_at TIMESTAMP
);

-- Миграция для старых баз данных (безопасно отработает благодаря IF NOT EXISTS)
ALTER TABLE IF EXISTS pending_mqtt_messages ADD COLUMN IF NOT EXISTS packet_id INT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS pending_mqtt_messages ADD COLUMN IF NOT EXISTS status VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_pending_packet_id ON pending_mqtt_messages (packet_id);
CREATE INDEX IF NOT EXISTS idx_pending_status ON pending_mqtt_messages (status);

-- Безопасная инициализация начального дашборда
INSERT INTO dashboard_settings (name, type)
SELECT 'Start dashboard', 'CARD'
WHERE NOT EXISTS (
  SELECT 1 FROM dashboard_settings WHERE name = 'Start dashboard'
);

-- ИСПРАВЛЕНО: удален сломанный блок VALUES, синтаксис теперь валиден для H2
INSERT INTO mqtt_settings (name)
SELECT 'Mqtt Settings'
WHERE NOT EXISTS (
  SELECT 1 FROM mqtt_settings WHERE name = 'Mqtt Settings'
);
