CREATE DATABASE IF NOT EXISTS fresh_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_message;

-- 1.消息模板配置
DROP TABLE IF EXISTS msg_template;
CREATE TABLE msg_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_type TINYINT NOT NULL COMMENT '1小程序订阅消息 2短信',
    template_id VARCHAR(100) COMMENT '微信/短信模板ID',
    title VARCHAR(100) NOT NULL COMMENT '模板标题',
    content TEXT COMMENT '模板内容占位符',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息推送模板';

-- 2.消息发送记录
DROP TABLE IF EXISTS msg_send_log;
CREATE TABLE msg_send_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    business_type TINYINT COMMENT '1订单变更 2售后通知 3团购成团',
    business_id BIGINT COMMENT '关联订单/售后ID',
    send_content TEXT COMMENT '实际推送内容',
    phone VARCHAR(11) COMMENT '接收手机号',
    openid VARCHAR(100) COMMENT '小程序用户openid',
    send_status TINYINT NOT NULL COMMENT '0待发送 1成功 2失败',
    err_msg VARCHAR(500) COMMENT '失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息推送日志';

-- 3.用户站内消息
DROP TABLE IF EXISTS user_inner_msg;
CREATE TABLE user_inner_msg (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    read_flag TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户站内通知';

-- ==================== 外键索引补充 ====================
-- msg_send_log 外键索引
ALTER TABLE msg_send_log ADD INDEX idx_user_id (user_id);
ALTER TABLE msg_send_log ADD INDEX idx_template_id (template_id);
ALTER TABLE msg_send_log ADD INDEX idx_business_id (business_id);

-- user_inner_msg 用户索引
ALTER TABLE user_inner_msg ADD INDEX idx_user_id (user_id);