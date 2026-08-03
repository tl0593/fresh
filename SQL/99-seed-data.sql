-- 初始化默认管理员账号（密码: admin123，MD5+盐加密）
USE fresh_user;

INSERT INTO sys_role (role_name, role_key, remark) VALUES ('超级管理员', 'admin', '系统管理员')
ON DUPLICATE KEY UPDATE role_name=role_name;

INSERT INTO sys_admin (username, password, real_name, role_id, status)
SELECT 'admin', '26af8841b53b54cfeaf1f3217c06f3b5', '系统管理员', id, 1
FROM sys_role WHERE role_key = 'admin' LIMIT 1
ON DUPLICATE KEY UPDATE username=username;

-- 默认管理员: admin / admin123

-- ========== 商品测试数据 ==========
USE fresh_goods;

-- 二级分类示例：左侧「父级 + 其子级」；sort 越小越靠前
INSERT INTO goods_category (id, parent_id, cat_name, icon, sort, status, del_flag)
VALUES (1, 0, '新鲜蔬菜', '', 1, 1, 0),
       (2, 1, '叶菜类', '', 1, 1, 0),
       (3, 1, '根茎类', '', 2, 1, 0),
       (4, 0, '新鲜水果', '', 2, 1, 0),
       (5, 4, '柑橘类', '', 1, 1, 0),
       (6, 4, '浆果类', '', 2, 1, 0)
ON DUPLICATE KEY UPDATE cat_name=VALUES(cat_name), parent_id=VALUES(parent_id), sort=VALUES(sort);

INSERT INTO goods (id, cat_id, goods_name, goods_img, goods_desc, origin_price, sale_price, total_stock, sale_count, unit, status, del_flag)
VALUES (1, 2, '有机菠菜', 'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09a.png', '新鲜有机菠菜', 8.00, 6.50, 100, 10, '份', 1, 0),
       (2, 2, '新鲜生菜', 'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09b.png', '当日采摘生菜', 5.00, 4.00, 80, 5, '份', 1, 0)
ON DUPLICATE KEY UPDATE goods_name=VALUES(goods_name), goods_img=VALUES(goods_img);

INSERT INTO goods_spec (id, goods_id, spec_name, spec_price, stock, is_default, del_flag)
VALUES (1, 1, '500g/份', 6.50, 100, 1, 0),
       (2, 1, '1kg/份', 12.00, 50, 0, 0),
       (3, 2, '400g/份', 4.00, 80, 1, 0)
ON DUPLICATE KEY UPDATE spec_name=VALUES(spec_name);

-- ========== 用户地址测试数据（user_id=1） ==========
USE fresh_user;

INSERT INTO user_address (id, user_id, name, phone, community, detail_addr, is_default, del_flag)
VALUES (1, 1, '张三', '13800138000', '阳光社区自提点', '3栋2单元101', 1, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ========== 团购活动测试数据 ==========
USE fresh_goods;

INSERT INTO group_activity (id, goods_id, spec_id, group_price, group_num, stock, start_time, end_time, group_desc, status, del_flag)
VALUES (1, 1, 1, 4.90, 3, 50,
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY),
        '有机菠菜限时团购，3人成团仅4.9元/份，新鲜直达！', 1, 0)
ON DUPLICATE KEY UPDATE group_price=VALUES(group_price), status=VALUES(status);

INSERT INTO seckill_activity (id, goods_id, spec_id, seckill_price, stock, start_time, end_time, status, del_flag)
VALUES (1, 1, 1, 5.50, 30,
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 0)
ON DUPLICATE KEY UPDATE seckill_price=VALUES(seckill_price), status=VALUES(status);

-- ========== 优惠券/营销测试数据 ==========
USE fresh_promotion;

INSERT INTO coupon_template (id, coupon_name, coupon_type, full_amount, reduce_amount, total_count, used_count, valid_day, start_time, end_time, limit_type, limit_num, status, del_flag)
VALUES
    (1, '新人无门槛券', 1, 0.00, 5.00, 1000, 0, 7,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, 2, 1, 0),
    (2, '满30减10券', 2, 30.00, 10.00, 500, 0, 15,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, 1, 1, 0),
    (3, '整点抢券8元券', 1, 0.00, 8.00, 200, 0, 3,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE coupon_name=VALUES(coupon_name), status=VALUES(status);

INSERT INTO full_reduce_activity (id, activity_name, full_amount, reduce_amount, target_type, target_cat_ids, start_time, end_time, stack_coupon, status, del_flag)
VALUES (1, '全场满50减5', 50.00, 5.00, 1, NULL,
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, 1, 0)
ON DUPLICATE KEY UPDATE activity_name=VALUES(activity_name);

INSERT INTO integral_coupon (id, template_id, cost_integral, daily_limit, total_stock, used_num, status, del_flag)
VALUES (1, 2, 100, 1, 100, 0, 1, 0)
ON DUPLICATE KEY UPDATE cost_integral=VALUES(cost_integral);

INSERT INTO seckill_coupon (id, template_id, start_hour, total_stock, used_num, activity_start, activity_end, status, del_flag)
VALUES (1, 3, HOUR(NOW()), 100, 0,
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 0)
ON DUPLICATE KEY UPDATE start_hour=HOUR(NOW()), status=VALUES(status);

INSERT INTO integral_lottery_prize (id, reward_type, reward_integral, reward_coupon_id, weight, cost_integral, status, del_flag)
VALUES
    (1, 1, 10, NULL, 70, 5, 1, 0),
    (2, 2, NULL, 1, 30, 5, 1, 0)
ON DUPLICATE KEY UPDATE weight=VALUES(weight);
