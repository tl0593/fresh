CREATE DATABASE IF NOT EXISTS fresh_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_ai;

-- 1.AI对话聊天记录
DROP TABLE IF EXISTS ai_chat_record;
CREATE TABLE ai_chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '小程序用户ID',
    session_key VARCHAR(100) NOT NULL COMMENT '会话标识(Redis缓存key)',
    user_msg TEXT NOT NULL COMMENT '用户提问',
    ai_reply TEXT NOT NULL COMMENT '大模型返回内容',
    chat_type TINYINT COMMENT '1普通咨询 2菜谱生成 3售后咨询',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服对话记录';

-- 2.AI图片识别理赔记录
DROP TABLE IF EXISTS ai_image_recognize;
CREATE TABLE ai_image_recognize (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    after_sale_id BIGINT NOT NULL COMMENT '关联售后工单',
    img_url VARCHAR(255) NOT NULL COMMENT '识别图片地址',
    raw_result TEXT COMMENT 'AI原始识别返回JSON',
    damage_level TINYINT COMMENT '1轻微 2中度 3重度',
    damage_ratio DECIMAL(5,2) COMMENT '损坏百分比',
    refund_amount DECIMAL(10,2) COMMENT '建议理赔金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生鲜坏图AI识别记录';

-- 3.AI团购文案生成记录
DROP TABLE IF EXISTS ai_group_text;
CREATE TABLE ai_group_text (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL COMMENT '操作商家/管理员ID',
    goods_id BIGINT NOT NULL,
    input_info TEXT COMMENT '商家输入商品基础信息',
    output_text TEXT COMMENT 'AI生成宣传文案',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI团购文案生成日志';

-- 4.AI知识库问答素材（后台配置）
DROP TABLE IF EXISTS ai_knowledge;
CREATE TABLE ai_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(300) NOT NULL COMMENT '标准问题',
    answer TEXT NOT NULL COMMENT 'AI固定回复',
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答知识库';

-- ==================== 外键索引补充 ====================
-- ai_chat_record 用户索引
ALTER TABLE ai_chat_record ADD INDEX idx_user_id (user_id);

-- ai_image_recognize 售后工单索引
ALTER TABLE ai_image_recognize ADD INDEX idx_after_sale_id (after_sale_id);

-- ai_group_text 管理员索引
ALTER TABLE ai_group_text ADD INDEX idx_admin_id (admin_id);

-- ai_group_text 商品索引
ALTER TABLE ai_group_text ADD INDEX idx_goods_id (goods_id);