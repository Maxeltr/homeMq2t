
CREATE TABLE IF NOT EXISTS dashboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    dashboard_number VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    card_number VARCHAR(50),
    subscription_topic VARCHAR(255),
    subscription_qos VARCHAR(50),
    subscription_data_name VARCHAR(255),
    subscription_data_type VARCHAR(50),
    display_data_jsonpath TEXT,
    publication_topic VARCHAR(255),
    publication_qos VARCHAR(50),
    publication_retain BOOLEAN,
    publication_data TEXT,
    publication_data_type VARCHAR(50),
    local_task_path VARCHAR(255),
    local_task_arguments VARCHAR(255),
    local_task_data_type VARCHAR(50),
    dashboard_id BIGINT,
    FOREIGN KEY (dashboard_id) REFERENCES dashboard(id) ON DELETE CASCADE
);
