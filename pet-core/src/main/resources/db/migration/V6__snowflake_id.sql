-- ==================== V6 Snowflake 分布式 ID 改造 ====================
-- 移除 12 张业务表的自增默认值和序列，改由应用层 Snowflake 算法生成 ID

-- sys_user
ALTER TABLE sys_user ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS sys_user_id_seq;

-- sys_role
ALTER TABLE sys_role ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS sys_role_id_seq;

-- sys_permission
ALTER TABLE sys_permission ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS sys_permission_id_seq;

-- product
ALTER TABLE product ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS product_id_seq;

-- license_code
ALTER TABLE license_code ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS license_code_id_seq;

-- operation_log
ALTER TABLE operation_log ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS operation_log_id_seq;

-- device_event
ALTER TABLE device_event ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS device_event_id_seq;

-- thing_model_property
ALTER TABLE thing_model_property ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS thing_model_property_id_seq;

-- thing_model_event
ALTER TABLE thing_model_event ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS thing_model_event_id_seq;

-- thing_model_service
ALTER TABLE thing_model_service ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS thing_model_service_id_seq;

-- telemetry_history
ALTER TABLE telemetry_history ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS telemetry_history_id_seq;

-- pending_device_secret
ALTER TABLE pending_device_secret ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS pending_device_secret_id_seq;

-- 注意：sys_user_role 和 sys_role_permission 关联表保留自增（由 JPA @JoinTable 管理）
