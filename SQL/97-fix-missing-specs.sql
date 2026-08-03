-- 为没有规格的在售商品补一条默认规格（解决「暂无规格可加购」）
USE fresh_goods;

INSERT INTO goods_spec (goods_id, spec_name, spec_price, stock, is_default, del_flag)
SELECT g.id,
       CONCAT('默认/', IFNULL(NULLIF(g.unit, ''), '份')),
       IFNULL(g.sale_price, 0),
       IFNULL(g.total_stock, 0),
       1,
       0
FROM goods g
WHERE g.del_flag = 0
  AND g.status = 1
  AND NOT EXISTS (
    SELECT 1 FROM goods_spec s
    WHERE s.goods_id = g.id AND s.del_flag = 0
  );
