USE fresh_goods;

ALTER TABLE goods_category ADD INDEX idx_parent_id (parent_id);
ALTER TABLE goods_category ADD INDEX idx_parent_status (parent_id, status, del_flag);
