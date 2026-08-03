-- 统一改用腾讯 CDN 图，避免 Unsplash 404 / 国内访问不稳
USE fresh_goods;

UPDATE goods SET goods_img='https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09a.png' WHERE id=1;
UPDATE goods SET goods_img='https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09b.png' WHERE id=2;
UPDATE goods SET goods_img='https://tdesign.gtimg.com/miniprogram/template/retail/goods/dz-3a.png' WHERE id=3;

UPDATE goods
SET goods_img='https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09a.png'
WHERE del_flag=0
  AND (goods_img LIKE '%unsplash%' OR goods_img LIKE '%/api/goods/upload/%');
