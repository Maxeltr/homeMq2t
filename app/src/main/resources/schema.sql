
CREATE SEQUENCE IF NOT EXISTS dashboard_number_seq
  START WITH 1
  INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS dashboard_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
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

CREATE SEQUENCE IF NOT EXISTS command_number_seq START WITH 1 INCREMENT BY 1;	

CREATE TABLE IF NOT EXISTS command_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subscription_topic VARCHAR(255),
    subscription_qos VARCHAR(50),
    publication_topic VARCHAR(255),
    publication_qos VARCHAR(50),
    publication_retain BOOLEAN,
    publication_data_type VARCHAR(50),
    path VARCHAR(255),
    arguments VARCHAR(255),
    number BIGINT DEFAULT NEXT VALUE FOR command_number_seq NOT NULL	
);

CREATE SEQUENCE IF NOT EXISTS component_number_seq START WITH 1 INCREMENT BY 1;	

CREATE TABLE IF NOT EXISTS component_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subscription_topic VARCHAR(255),
    subscription_qos VARCHAR(50),
    publication_topic VARCHAR(255),
    publication_qos VARCHAR(50),
    publication_retain BOOLEAN,
    publication_data_type VARCHAR(50),
    publication_local_card_id VARCHAR(255),
    provider VARCHAR(255),
    provider_args VARCHAR(255),
    number BIGINT DEFAULT NEXT VALUE FOR component_number_seq NOT NULL	
);

CREATE SEQUENCE IF NOT EXISTS startup_task_number_seq START WITH 1 INCREMENT BY 1;	

CREATE TABLE IF NOT EXISTS startup_task_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(255),
    arguments VARCHAR(255),
    number BIGINT DEFAULT NEXT VALUE FOR startup_task_number_seq NOT NULL	
);

CREATE TABLE IF NOT EXISTS mqtt_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    host VARCHAR(255),
    port VARCHAR(50),
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
    reconnect BOOLEAN
);

INSERT INTO dashboard_settings (name)
SELECT 'Start dashboard'
WHERE NOT EXISTS (SELECT 1 FROM dashboard_settings);
VALUES
  ('Start dashboard');

-- INSERT INTO card_settings (
--     name,
--     subscription_topic,
--     subscription_qos,
--     subscription_data_name,
--     subscription_data_type,
--     display_data_jsonpath,
--     publication_topic,
--     publication_qos,
--     publication_retain,
--     publication_data,
--     publication_data_type,
--     local_task_path,
--     local_task_arguments,
--     local_task_data_type,
--     dashboard_id  
-- ) VALUES (
--     'Тестовая карточка',  --card name
--     'sensors/temperature',  --subscription_topic
--     'AT_MOST_ONCE',
--     'Current temperature',  -- subscription_data_name (пустое значение)
--     'application/json',
--     '$.temperature',  -- display_data_jsonpath (пустое значение)
--     'sensors/temperature',  -- publication_topic
--     'AT_MOST_ONCE',
--     FALSE,  -- publication_retain
--     '{
--        "deviceId":"device-001",
--        "timestamp":"2025-07-26T12:00:00Z",
--        "temperature":23.5,
--        "humidity":45.2,
--        "status":"OK"
--     }',  -- publication_data
--     'application/json',
--     NULL,  -- local_task_path (пустое значение)
--     NULL,  -- local_task_arguments (пустое значение)
--     NULL,  -- local_task_data_type (пустое значение)
--     1   -- dashboard_id 
-- );

