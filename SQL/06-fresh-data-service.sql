CREATE DATABASE IF NOT EXISTS fresh_data DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_data;

-- 1.用户行为埋点日志（MQ异步写入）
DROP TABLE IF EXISTS user_behavior_log;
CREATE TABLE user_behavior_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    behavior_type TINYINT NOT NULL COMMENT '1浏览商品 2加购 3下单 4收藏',
    goods_id BIGINT COMMENT '操作商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time(user_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为埋点日志';

-- 2.每日统计汇总表（大屏数据源）
DROP TABLE IF EXISTS daily_stat;
CREATE TABLE daily_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',
    new_user INT DEFAULT 0 COMMENT '新增用户',
    active_user INT DEFAULT 0 COMMENT '活跃用户',
    order_count INT DEFAULT 0 COMMENT '订单总数',
    order_amount DECIMAL(12,2) DEFAULT 0 COMMENT '当日成交总额',
    group_success_num INT DEFAULT 0 COMMENT '成团数量',
    after_sale_num INT DEFAULT 0 COMMENT '售后工单数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stat_date(stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日平台数据统计';

-- 3.商品销量统计表
DROP TABLE IF EXISTS goods_sales_stat;
CREATE TABLE goods_sales_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL,
    goods_id BIGINT NOT NULL,
    sale_num INT DEFAULT 0 COMMENT '销售件数',
    sale_amount DECIMAL(12,2) DEFAULT 0,
    UNIQUE KEY uk_date_goods(stat_date,goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品日销量统计';

-- 4.行为日志归档表（30天前冷数据）
DROP TABLE IF EXISTS user_behavior_log_archive;
CREATE TABLE user_behavior_log_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    behavior_type TINYINT NOT NULL COMMENT '1浏览商品 2加购 3下单 4收藏 5AI咨询',
    goods_id BIGINT COMMENT '操作商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time(user_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为埋点归档';