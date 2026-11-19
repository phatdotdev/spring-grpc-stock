CREATE DATABASE IF NOT EXISTS stock_trading
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE stock_trading;

CREATE TABLE IF NOT EXISTS stocks (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      stock_name VARCHAR(255) NOT NULL,
    price DOUBLE NOT NULL,
    last_updated DATETIME(6),
    currency VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_name (stock_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO stocks (stock_name, price, last_updated, currency) VALUES
    ('AAPL', 228.50, NOW(), 'USD'),
    ('GOOGL', 176.20, NOW(), 'USD'),
    ('MSFT', 425.80, NOW(), 'USD'),
    ('NVDA', 142.65, NOW(), 'USD'),
    ('TSLA', 315.40, NOW(), 'USD'),
    ('FPT', 136500.0, NOW(), 'VND'),
    ('VCB', 92800.0, NOW(), 'VND');