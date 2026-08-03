CREATE DATABASE IF NOT EXISTS fresh_comment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_comment;

-- 商品评价主表
DROP TABLE IF EXISTS goods_comment;
CREATE TABLE goods_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    user_id BIGINT NOT NULL COMMENT '评价用户ID',
    order_item_id BIGINT NOT NULL COMMENT '订单项ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    goods_id BIGINT NOT NULL COMMENT '评价商品ID',
    spec_id BIGINT NOT NULL COMMENT '商品规格ID',
    score TINYINT NOT NULL COMMENT '评分1-5星',
    content TEXT COMMENT '评价文字内容',
    status TINYINT DEFAULT 1 COMMENT '0隐藏 1正常展示',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_goods (goods_id),
    INDEX idx_user (user_id),
    INDEX idx_order_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价主表';

-- 评价图片表（一对多，一条评价多张实拍图）
DROP TABLE IF EXISTS comment_image;
CREATE TABLE comment_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL COMMENT '关联评价ID',
    img_url VARCHAR(255) NOT NULL COMMENT '图片OSS地址',
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价实拍图片';

-- 商家评价回复表
DROP TABLE IF EXISTS comment_reply;
CREATE TABLE comment_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL COMMENT '评价ID',
    admin_id BIGINT NOT NULL COMMENT '回复商家/管理员ID',
    reply_content TEXT NOT NULL COMMENT '回复内容',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家评价回复';