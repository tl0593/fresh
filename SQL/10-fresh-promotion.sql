CREATE DATABASE IF NOT EXISTS fresh_promotion DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_promotion;

-- 1.优惠券模板（无门槛/满减/品类/团购券）
DROP TABLE IF EXISTS coupon_template;
CREATE TABLE coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_type TINYINT NOT NULL COMMENT '1无门槛 2满减 3品类专享 4团购专用',
    full_amount DECIMAL(10,2) DEFAULT 0 COMMENT '满减门槛，0=无门槛',
    reduce_amount DECIMAL(10,2) NOT NULL COMMENT '抵扣金额',
    total_count INT NOT NULL COMMENT '发放总数量',
    used_count INT DEFAULT 0 COMMENT '已核销数量',
    valid_day INT NOT NULL COMMENT '领取后有效天数',
    start_time DATETIME NOT NULL COMMENT '发放开始',
    end_time DATETIME NOT NULL COMMENT '发放结束',
    limit_type TINYINT DEFAULT 1 COMMENT '1单人限领N 2不限',
    limit_num INT DEFAULT 1 COMMENT '单人领取上限',
    status TINYINT DEFAULT 1 COMMENT '0未开始 1进行中 2已结束 3下架',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status(status)
)ENGINE=InnoDB COMMENT='优惠券模板';

-- 2.用户持有优惠券
DROP TABLE IF EXISTS user_coupon;
CREATE TABLE user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '券模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receive_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    valid_start DATETIME NOT NULL COMMENT '有效期开始',
    valid_end DATETIME NOT NULL COMMENT '有效期截止',
    use_status TINYINT DEFAULT 0 COMMENT '0未使用 1已核销 2已过期',
    order_no VARCHAR(32) COMMENT '核销订单号',
    del_flag TINYINT DEFAULT 0,
    INDEX idx_user(user_id),
    INDEX idx_template(template_id),
    INDEX idx_valid(valid_end,use_status)
)ENGINE=InnoDB COMMENT='用户领取优惠券';

-- 3.满减活动（全场/分类满减）
DROP TABLE IF EXISTS full_reduce_activity;
CREATE TABLE full_reduce_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_name VARCHAR(100) NOT NULL,
    full_amount DECIMAL(10,2) NOT NULL COMMENT '满XX元',
    reduce_amount DECIMAL(10,2) NOT NULL COMMENT '减XX元',
    target_type TINYINT NOT NULL COMMENT '1全场 2指定分类',
    target_cat_ids VARCHAR(500) COMMENT '绑定分类ID逗号分隔',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    stack_coupon TINYINT DEFAULT 1 COMMENT '1可叠加优惠券 0不可叠加',
    status TINYINT DEFAULT 1,
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
)ENGINE=InnoDB COMMENT='平台满减活动';

-- 4.优惠券核销流水
DROP TABLE IF EXISTS coupon_use_log;
CREATE TABLE coupon_use_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_coupon_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    deduct_money DECIMAL(10,2) NOT NULL COMMENT '实际抵扣金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order(order_no),
    INDEX idx_user_id(user_id),
    INDEX idx_user_coupon_id(user_coupon_id),
    INDEX idx_template_id(template_id)
)ENGINE=InnoDB COMMENT='优惠券核销日志';

-- 5.积分兑换优惠券配置
DROP TABLE IF EXISTS integral_coupon;
CREATE TABLE integral_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '关联优惠券模板',
    cost_integral INT NOT NULL COMMENT '兑换所需积分',
    daily_limit INT NOT NULL COMMENT '单人每日兑换上限',
    total_stock INT NOT NULL COMMENT '总库存',
    used_num INT DEFAULT 0 COMMENT '已兑换数量',
    status TINYINT DEFAULT 1 COMMENT '0下架 1正常',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
)ENGINE=InnoDB COMMENT '积分兑换优惠券配置';

-- 6.整点限时抢券活动
DROP TABLE IF EXISTS seckill_coupon;
CREATE TABLE seckill_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '对应优惠券',
    start_hour INT NOT NULL COMMENT '每日开抢整点(0-23)',
    total_stock INT NOT NULL COMMENT '活动总库存',
    used_num INT DEFAULT 0 COMMENT '已抢数量',
    activity_start DATETIME NOT NULL COMMENT '活动周期开始',
    activity_end DATETIME NOT NULL COMMENT '活动周期结束',
    status TINYINT DEFAULT 0 COMMENT '0未开启 1正常 2结束',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
)ENGINE=InnoDB COMMENT '整点限时抢券活动';

-- 7.积分抽奖奖品池
DROP TABLE IF EXISTS integral_lottery_prize;
CREATE TABLE integral_lottery_prize (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reward_type TINYINT NOT NULL COMMENT '1积分奖品 2优惠券奖品',
    reward_integral INT COMMENT '奖品积分数量',
    reward_coupon_id BIGINT COMMENT '奖品优惠券ID',
    weight INT NOT NULL COMMENT '中奖权重，数字越大概率越高',
    cost_integral INT NOT NULL COMMENT '单次抽奖消耗积分',
    status TINYINT DEFAULT 1,
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
)ENGINE=InnoDB COMMENT '积分抽奖奖品配置';