CREATE DATABASE IF NOT EXISTS fresh_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_gateway;

-- 1.接口限流配置表
DROP TABLE IF EXISTS limit_config;
CREATE TABLE limit_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_path VARCHAR(200) NOT NULL COMMENT '接口路径',
    limit_type TINYINT NOT NULL COMMENT '1全局限流 2单IP限流',
    limit_count INT NOT NULL COMMENT '单位时间访问上限',
    time_second INT NOT NULL DEFAULT 60 COMMENT '统计周期秒',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关限流规则';

-- 2.黑名单IP表
DROP TABLE IF EXISTS black_ip;
CREATE TABLE black_ip (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip VARCHAR(50) NOT NULL UNIQUE,
    reason VARCHAR(200) COMMENT '拉黑原因 刷单/恶意请求',
    expire_time DATETIME COMMENT '过期时间 NULL永久拉黑',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关IP黑名单';

-- 3.网关访问日志
DROP TABLE IF EXISTS gateway_access_log;
CREATE TABLE gateway_access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_ip VARCHAR(50),
    request_url VARCHAR(255),
    request_method VARCHAR(10),
    user_id BIGINT,
    token VARCHAR(200),
    response_code INT,
    cost_time INT COMMENT '请求耗时ms',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关请求日志';

-- 默认限流规则：单 IP 单接口 1 分钟 100 次
INSERT INTO limit_config (api_path, limit_type, limit_count, time_second, status)
VALUES ('/api/', 2, 100, 60, 1);