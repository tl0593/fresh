CREATE DATABASE IF NOT EXISTS fresh_common DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_common;

-- 1.系统字典表
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type VARCHAR(50) NOT NULL COMMENT '字典类型编码',
    dict_label VARCHAR(100) NOT NULL COMMENT '展示名称',
    dict_value VARCHAR(100) NOT NULL COMMENT '存储值',
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通用字典';

-- 2.系统操作日志
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL COMMENT '操作管理员ID',
    oper_module VARCHAR(50) COMMENT '操作模块',
    oper_type VARCHAR(30) COMMENT '新增/修改/删除/审核',
    oper_content TEXT COMMENT '操作详情',
    ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';

-- 3.全局异常日志
DROP TABLE IF EXISTS sys_error_log;
CREATE TABLE sys_error_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(50) NOT NULL COMMENT '异常微服务名称',
    error_msg TEXT NOT NULL COMMENT '异常堆栈信息',
    request_param TEXT COMMENT '请求参数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微服务全局异常日志';