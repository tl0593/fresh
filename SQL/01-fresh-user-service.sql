-- 数据库：fresh_user
CREATE DATABASE IF NOT EXISTS fresh_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fresh_user;

-- 1.平台管理员&商家账号表
DROP TABLE IF EXISTS sys_admin;
CREATE TABLE sys_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '加密密码',
    real_name VARCHAR(30) COMMENT '真实姓名',
    phone VARCHAR(11) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像地址',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1正常',
    del_flag TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删 1删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员/商家账号';

-- 2.角色表 RBAC权限
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识 admin/merchant/user',
    remark VARCHAR(200) COMMENT '角色备注',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3.菜单权限表
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    path VARCHAR(100) COMMENT '路由地址',
    perms VARCHAR(100) COMMENT '权限标识',
    sort INT DEFAULT 0 COMMENT '排序',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限';

-- 4.角色菜单关联表
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id,menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- 5.小程序C端用户表
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(100) UNIQUE NOT NULL COMMENT '微信openid',
    nick_name VARCHAR(50) COMMENT '微信昵称',
    avatar VARCHAR(255) COMMENT '微信头像',
    phone VARCHAR(11) COMMENT '绑定手机号',
    integral INT DEFAULT 0 COMMENT '会员积分',
    frozen_integral INT DEFAULT 0 COMMENT '冻结积分(下单预扣)',
    status TINYINT DEFAULT 1 COMMENT '0禁用 1正常',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户';

-- 6.用户收货地址表
DROP TABLE IF EXISTS user_address;
CREATE TABLE user_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '小程序用户ID',
    name VARCHAR(30) NOT NULL COMMENT '收货人',
    phone VARCHAR(11) NOT NULL,
    community VARCHAR(100) NOT NULL COMMENT '社区名称(自提点)',
    detail_addr VARCHAR(200) COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '0否 1默认地址',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货/自提地址';

-- 7.用户购物车（Redis缓存为主，数据库持久兜底）
DROP TABLE IF EXISTS user_cart;
CREATE TABLE user_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    spec_id BIGINT NOT NULL COMMENT '商品规格ID',
    num INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    selected TINYINT DEFAULT 1 COMMENT '是否选中结算',
    del_flag TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户购物车';

-- 8.积分变动记录表
DROP TABLE IF EXISTS user_integral_log;
CREATE TABLE user_integral_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    change_num INT NOT NULL COMMENT '变动积分 正数增加 负数扣除',
    type TINYINT NOT NULL COMMENT '1下单获得 2售后扣除 3活动兑换',
    order_id BIGINT COMMENT '关联订单ID',
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- ==================== 积分拓展表：签到、抽奖、积分兑换流水 ====================
-- 每日签到表
DROP TABLE IF EXISTS user_sign;
CREATE TABLE user_sign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '小程序用户ID',
    sign_date DATE NOT NULL COMMENT '签到日期',
    integral INT NOT NULL COMMENT '本次签到发放积分',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id,sign_date) COMMENT '一人一天仅签到一次',
    INDEX idx_uid(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户每日签到记录';

-- 积分抽奖记录表
DROP TABLE IF EXISTS integral_lottery_log;
CREATE TABLE integral_lottery_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cost_integral INT NOT NULL COMMENT '本次消耗积分',
    reward_type TINYINT NOT NULL COMMENT '1积分奖励 2优惠券奖励',
    reward_integral INT COMMENT '中奖积分数量',
    reward_coupon_id BIGINT COMMENT '中奖优惠券ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uid(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '积分抽奖中奖记录';

-- 积分兑换优惠券流水
DROP TABLE IF EXISTS integral_exchange_log;
CREATE TABLE integral_exchange_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cost_integral INT NOT NULL COMMENT '兑换消耗积分',
    template_id BIGINT NOT NULL COMMENT '兑换的优惠券模板ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uid(user_id),
    INDEX idx_template_id(template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '积分兑换记录';

-- ==================== 外键索引补充 ====================
-- sys_menu 父菜单索引
ALTER TABLE sys_menu ADD INDEX idx_parent_id (parent_id);

-- user_address 用户索引
ALTER TABLE user_address ADD INDEX idx_user_id (user_id);

-- user_cart 唯一约束+用户索引
ALTER TABLE user_cart ADD UNIQUE KEY uk_user_goods_spec (user_id, goods_id, spec_id);
ALTER TABLE user_cart ADD INDEX idx_user_id (user_id);

-- user_integral_log 用户索引
ALTER TABLE user_integral_log ADD INDEX idx_user_id (user_id);