-- ==================== V4 设备-产品关联 + 授权码预绑定设备 ====================

-- device 表新增 product_key（兼容已有数据，允许 NULL）
ALTER TABLE device ADD COLUMN IF NOT EXISTS product_key VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_device_product_key ON device(product_key);

-- license_code 表新增 product_key
ALTER TABLE license_code ADD COLUMN IF NOT EXISTS product_key VARCHAR(50);

-- 设备 SN 序列（全局自增，保证唯一）
CREATE SEQUENCE IF NOT EXISTS device_sn_seq START WITH 1 INCREMENT BY 1;
