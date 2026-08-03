CREATE DATABASE IF NOT EXISTS fresh_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_order;

-- 1.订单主表
DROP TABLE IF EXISTS order_main;
CREATE TABLE order_main (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) UNIQUE NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '小程序用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    pay_type TINYINT COMMENT '1微信支付',
    pay_time DATETIME COMMENT '支付时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1待自提 2已完成 3已取消 4售后中',
    address_id BIGINT NOT NULL COMMENT '自提点地址ID',
    receiver_name VARCHAR(30) NOT NULL COMMENT '收货人姓名(快照)',
    receiver_phone VARCHAR(11) NOT NULL COMMENT '收货人手机(快照)',
    community VARCHAR(100) NOT NULL COMMENT '自提社区(快照)',
    detail_address VARCHAR(200) COMMENT '详细地址(快照)',
    group_activity_id BIGINT COMMENT '关联团购活动ID',
    group_record_id BIGINT COMMENT '拼团记录ID',
    seckill_activity_id BIGINT COMMENT '关联秒杀活动ID',
    coupon_id BIGINT COMMENT '使用的用户优惠券ID',
    coupon_deduct DECIMAL(10,2) DEFAULT 0 COMMENT '优惠券抵扣金额',
    fullreduce_deduct DECIMAL(10,2) DEFAULT 0 COMMENT '满减活动抵扣金额',
    integral_used_count INT DEFAULT 0 COMMENT '使用的积分数量',
    integral_deduct_amount DECIMAL(10,2) DEFAULT 0 COMMENT '积分抵扣金额',
    timeout_cancel TINYINT DEFAULT 0 COMMENT '是否超时自动取消',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 2.订单商品明细表
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    spec_id BIGINT NOT NULL,
    activity_type TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2团购 3秒杀',
    activity_id BIGINT COMMENT '具体活动ID(团购/秒杀活动表ID)',
    goods_name VARCHAR(100) NOT NULL,
    goods_img VARCHAR(255),
    price DECIMAL(10,2) NOT NULL COMMENT '下单单价',
    num INT NOT NULL,
    sub_total DECIMAL(10,2) NOT NULL COMMENT '小计',
    is_commented TINYINT DEFAULT 0 COMMENT '0未评价 1已评价',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细';

-- 3.拼团记录表
DROP TABLE IF EXISTS group_record;
CREATE TABLE group_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_activity_id BIGINT NOT NULL,
    leader_user_id BIGINT NOT NULL COMMENT '团长用户ID',
    target_num INT NOT NULL COMMENT '需成团人数',
    current_num INT DEFAULT 1 COMMENT '当前参与人数',
    expire_time DATETIME NOT NULL COMMENT '拼团过期时间',
    status TINYINT NOT NULL COMMENT '0拼团中 1已成团 2超时解散',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团记录';

-- 4.拼团参与人记录
DROP TABLE IF EXISTS group_join;
CREATE TABLE group_join (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_record_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL COMMENT '对应订单',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_record_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团参与明细';

-- 5.售后工单表（AI图像识别理赔）
DROP TABLE IF EXISTS after_sale;
CREATE TABLE after_sale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL COMMENT '售后商品明细ID',
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    damage_img TEXT COMMENT '用户上传损坏图片多图逗号分隔',
    ai_damage_level TINYINT COMMENT 'AI识别损坏等级 1轻微 2中度 3重度',
    ai_rate DECIMAL(5,2) COMMENT '损坏比例0~100',
    ai_refund_money DECIMAL(10,2) COMMENT 'AI自动核算理赔金额',
    actual_refund_money DECIMAL(10,2) COMMENT '管理员实际退款金额',
    audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1审核通过 2驳回',
    audit_admin_id BIGINT COMMENT '审核管理员ID',
    refund_time DATETIME COMMENT '退款完成时间',
    remark VARCHAR(300),
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后理赔工单';

-- 6.支付记录表
DROP TABLE IF EXISTS pay_log;
CREATE TABLE pay_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    out_trade_no VARCHAR(64) COMMENT '微信支付单号',
    pay_amount DECIMAL(10,2) NOT NULL,
    pay_status TINYINT COMMENT '0待支付 1支付成功 2支付失败',
    callback_content TEXT COMMENT '支付回调原始数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志';

-- ==================== 外键索引补充 ====================
-- order_item 外键索引
ALTER TABLE order_item ADD INDEX idx_order_id (order_id);
ALTER TABLE order_item ADD INDEX idx_goods_id (goods_id);

-- group_record 外键索引
ALTER TABLE group_record ADD INDEX idx_group_activity_id (group_activity_id);
ALTER TABLE group_record ADD INDEX idx_leader_user_id (leader_user_id);

-- group_join 外键索引
ALTER TABLE group_join ADD INDEX idx_group_record_id (group_record_id);
ALTER TABLE group_join ADD INDEX idx_order_id (order_id);

-- after_sale 外键索引
ALTER TABLE after_sale ADD INDEX idx_order_item_id (order_item_id);
ALTER TABLE after_sale ADD INDEX idx_user_id (user_id);

-- pay_log 外键索引
ALTER TABLE pay_log ADD INDEX idx_order_id (order_id);

-- order_main 外键索引
ALTER TABLE order_main ADD INDEX idx_user_id (user_id);
ALTER TABLE order_main ADD INDEX idx_group_activity_id (group_activity_id);
ALTER TABLE order_main ADD INDEX idx_seckill_activity_id (seckill_activity_id);