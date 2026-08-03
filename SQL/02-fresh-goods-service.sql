CREATE DATABASE IF NOT EXISTS fresh_goods DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_goods;

-- 1.商品分类表
DROP TABLE IF EXISTS goods_category;
CREATE TABLE goods_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    cat_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    icon VARCHAR(255) COMMENT '分类图标',
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '0停用 1启用',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生鲜商品分类';

-- 2.商品主表
DROP TABLE IF EXISTS goods;
CREATE TABLE goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cat_id BIGINT NOT NULL COMMENT '分类ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    goods_img VARCHAR(255) NOT NULL COMMENT '主图',
    goods_desc TEXT COMMENT '商品详情',
    origin_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    sale_price DECIMAL(10,2) NOT NULL COMMENT '售价',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    sale_count INT DEFAULT 0 COMMENT '销量',
    unit VARCHAR(20) COMMENT '单位：斤/份/盒',
    status TINYINT DEFAULT 0 COMMENT '0下架 1上架',
    is_seckill TINYINT DEFAULT 0 COMMENT '是否秒杀商品',
    is_group TINYINT DEFAULT 0 COMMENT '是否参与团购',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生鲜商品主表';

-- 3.商品规格表
DROP TABLE IF EXISTS goods_spec;
CREATE TABLE goods_spec (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL,
    spec_name VARCHAR(50) NOT NULL COMMENT '规格名称 500g/2斤',
    spec_price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    is_default TINYINT DEFAULT 0 COMMENT '0否 1默认规格',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格';

-- 4.商品图集
DROP TABLE IF EXISTS goods_image;
CREATE TABLE goods_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL,
    img_url VARCHAR(255) NOT NULL,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品轮播图片';

-- 5.社区团购活动表
DROP TABLE IF EXISTS group_activity;
CREATE TABLE group_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL,
    spec_id BIGINT NOT NULL COMMENT '商品规格ID',
    group_price DECIMAL(10,2) NOT NULL COMMENT '团购价',
    group_num INT NOT NULL DEFAULT 3 COMMENT '成团人数',
    stock INT NOT NULL DEFAULT 0 COMMENT '团购库存',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    group_desc TEXT COMMENT 'AI生成团购宣传文案',
    status TINYINT DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区团购活动';

-- 6.秒杀活动表
DROP TABLE IF EXISTS seckill_activity;
CREATE TABLE seckill_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL,
    spec_id BIGINT NOT NULL COMMENT '商品规格ID',
    seckill_price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT DEFAULT 0,
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时秒杀';

-- ==================== 外键索引补充 ====================
-- goods 分类索引
ALTER TABLE goods ADD INDEX idx_cat_id (cat_id);

-- goods_category 父级索引（按父级查子分类；现网幂等脚本见 11-index-category-parent.sql）
ALTER TABLE goods_category ADD INDEX idx_parent_id (parent_id);

-- goods_spec 商品索引
ALTER TABLE goods_spec ADD INDEX idx_goods_id (goods_id);

-- goods_image 商品索引
ALTER TABLE goods_image ADD INDEX idx_goods_id (goods_id);

-- group_activity 商品索引
ALTER TABLE group_activity ADD INDEX idx_goods_id (goods_id);

-- seckill_activity 商品索引
ALTER TABLE seckill_activity ADD INDEX idx_goods_id (goods_id);