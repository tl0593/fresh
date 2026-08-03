-- 售后审核状态索引（待办列表）
USE fresh_order;
ALTER TABLE after_sale ADD INDEX idx_audit_status (audit_status);
