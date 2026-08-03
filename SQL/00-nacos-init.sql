-- Nacos 初始化数据库
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nacos_config;

-- 从 Nacos 2.1.2 的 schema.sql 复制的核心表结构
CREATE TABLE IF NOT EXISTS config_info (
    id bigint NOT NULL AUTO_INCREMENT,
    data_id varchar(255) NOT NULL,
    group_id varchar(128) NOT NULL,
    content longtext NOT NULL,
    md5 varchar(32) DEFAULT NULL,
    gmt_create datetime NOT NULL DEFAULT current_timestamp,
    gmt_modified datetime NOT NULL DEFAULT current_timestamp,
    src_ip varchar(50) DEFAULT NULL,
    src_user varchar(50) DEFAULT NULL,
    op_type char(10) DEFAULT NULL,
    tenant_id varchar(128) NOT NULL DEFAULT '',
    app_name varchar(128) DEFAULT '',
    tag_id varchar(128) DEFAULT NULL,
    type varchar(64) DEFAULT 'text',
    extra_info text,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_datagroup (data_id, group_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS config_info_history (
    id bigint NOT NULL AUTO_INCREMENT,
    data_id varchar(255) NOT NULL,
    group_id varchar(128) NOT NULL,
    content longtext NOT NULL,
    md5 varchar(32) DEFAULT NULL,
    gmt_create datetime NOT NULL DEFAULT current_timestamp,
    gmt_modified datetime NOT NULL DEFAULT current_timestamp,
    src_ip varchar(50) DEFAULT NULL,
    src_user varchar(50) DEFAULT NULL,
    op_type char(10) DEFAULT NULL,
    tenant_id varchar(128) NOT NULL DEFAULT '',
    app_name varchar(128) DEFAULT '',
    tag_id varchar(128) DEFAULT NULL,
    type varchar(64) DEFAULT 'text',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS his_config_info (
    id bigint NOT NULL AUTO_INCREMENT,
    data_id varchar(255) NOT NULL,
    group_id varchar(128) NOT NULL,
    content longtext NOT NULL,
    md5 varchar(32) DEFAULT NULL,
    gmt_create datetime NOT NULL DEFAULT current_timestamp,
    gmt_modified datetime NOT NULL DEFAULT current_timestamp,
    src_ip varchar(50) DEFAULT NULL,
    src_user varchar(50) DEFAULT NULL,
    op_type char(10) DEFAULT NULL,
    tenant_id varchar(128) NOT NULL DEFAULT '',
    app_name varchar(128) DEFAULT '',
    tag_id varchar(128) DEFAULT NULL,
    type varchar(64) DEFAULT 'text',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_info (
    id bigint NOT NULL AUTO_INCREMENT,
    kp varchar(128) NOT NULL,
    tenant_id varchar(128) NOT NULL,
    tenant_name varchar(128) NOT NULL,
    tenant_desc varchar(256) DEFAULT NULL,
    create_source varchar(32) DEFAULT NULL,
    gmt_create bigint NOT NULL,
    gmt_modified bigint NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_info (
    id bigint NOT NULL AUTO_INCREMENT,
    username varchar(50) NOT NULL,
    password varchar(500) NOT NULL,
    salt varchar(50) NOT NULL,
    avatar varchar(255) DEFAULT NULL,
    email varchar(255) DEFAULT NULL,
    mobile varchar(255) DEFAULT NULL,
    status tinyint NOT NULL DEFAULT '0',
    deleted tinyint NOT NULL DEFAULT '0',
    gmt_create datetime DEFAULT NULL,
    gmt_modified datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_info (
    id bigint NOT NULL AUTO_INCREMENT,
    role_name varchar(50) NOT NULL,
    role_key varchar(50) NOT NULL,
    status tinyint NOT NULL DEFAULT '0',
    deleted tinyint NOT NULL DEFAULT '0',
    gmt_create datetime DEFAULT NULL,
    gmt_modified datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化默认用户 nacos/nacos
INSERT INTO user_info (username, password, salt, status, deleted, gmt_create, gmt_modified)
VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUuHpruqOV3wLyQ.5sOHsDVp2vGDMOk2FdGq3dC8NyuC', 'nacos', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE gmt_modified = NOW();

INSERT INTO role_info (role_name, role_key, status, deleted, gmt_create, gmt_modified)
VALUES ('ROLE_ADMIN', 'ROLE_ADMIN', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE gmt_modified = NOW();

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM user_info u, role_info r
WHERE u.username = 'nacos' AND r.role_key = 'ROLE_ADMIN'
ON DUPLICATE KEY UPDATE user_role.user_id = user_role.user_id;
