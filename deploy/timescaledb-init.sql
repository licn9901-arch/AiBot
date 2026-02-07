-- ==================== TimescaleDB 时序库初始化 ====================
-- 此脚本在 TimescaleDB 实例上执行，不通过 Flyway 管理

-- 启用 TimescaleDB 扩展
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- 创建 schema
CREATE SCHEMA IF NOT EXISTS deskpet_ts;
SET search_path TO deskpet_ts, public;

-- ==================== 时序表 ====================

-- 1. 遥测历史
CREATE TABLE ts_telemetry (
    device_id       TEXT        NOT NULL,
    telemetry       JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
SELECT create_hypertable('ts_telemetry', by_range('created_at', INTERVAL '7 days'));
CREATE INDEX idx_ts_telemetry_device ON ts_telemetry(device_id, created_at DESC);

-- 2. 设备事件
CREATE TABLE ts_device_event (
    device_id       VARCHAR(64) NOT NULL,
    event_id        VARCHAR(50) NOT NULL,
    event_type      VARCHAR(20) NOT NULL,
    params          JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
SELECT create_hypertable('ts_device_event', by_range('created_at', INTERVAL '7 days'));
CREATE INDEX idx_ts_device_event_device ON ts_device_event(device_id, created_at DESC);
CREATE INDEX idx_ts_device_event_type ON ts_device_event(event_type, created_at DESC);

-- 3. 设备上下线历史
CREATE TABLE ts_device_session (
    device_id           TEXT        NOT NULL,
    online              BOOLEAN     NOT NULL,
    gateway_instance_id TEXT,
    ip                  TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
SELECT create_hypertable('ts_device_session', by_range('created_at', INTERVAL '7 days'));
CREATE INDEX idx_ts_device_session_device ON ts_device_session(device_id, created_at DESC);

-- 4. 指令记录
CREATE TABLE ts_device_command (
    req_id          TEXT        NOT NULL,
    device_id       TEXT        NOT NULL,
    type            TEXT        NOT NULL,
    payload         JSONB,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
SELECT create_hypertable('ts_device_command', by_range('created_at', INTERVAL '7 days'));
CREATE INDEX idx_ts_device_command_device ON ts_device_command(device_id, created_at DESC);

-- 5. 操作日志
CREATE TABLE ts_operation_log (
    user_id         BIGINT,
    device_id       VARCHAR(64),
    action          VARCHAR(50) NOT NULL,
    payload         JSONB,
    ip              VARCHAR(50),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
SELECT create_hypertable('ts_operation_log', by_range('created_at', INTERVAL '30 days'));
CREATE INDEX idx_ts_operation_log_user ON ts_operation_log(user_id, created_at DESC);
CREATE INDEX idx_ts_operation_log_action ON ts_operation_log(action, created_at DESC);

-- 6. 应用日志
CREATE TABLE ts_app_log (
    log_time        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    level           VARCHAR(10) NOT NULL,
    logger          VARCHAR(255),
    thread          VARCHAR(100),
    message         TEXT,
    exception       TEXT,
    mdc             JSONB
);
SELECT create_hypertable('ts_app_log', by_range('log_time', INTERVAL '1 day'));
CREATE INDEX idx_ts_app_log_level ON ts_app_log(level, log_time DESC);
CREATE INDEX idx_ts_app_log_logger ON ts_app_log(logger, log_time DESC);

-- ==================== 数据保留策略 ====================

SELECT add_retention_policy('ts_telemetry', INTERVAL '90 days');
SELECT add_retention_policy('ts_device_event', INTERVAL '180 days');
SELECT add_retention_policy('ts_device_session', INTERVAL '90 days');
SELECT add_retention_policy('ts_device_command', INTERVAL '90 days');
SELECT add_retention_policy('ts_operation_log', INTERVAL '365 days');
SELECT add_retention_policy('ts_app_log', INTERVAL '30 days');

-- ==================== 压缩策略 ====================

ALTER TABLE ts_telemetry SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id'
);
SELECT add_compression_policy('ts_telemetry', INTERVAL '7 days');

ALTER TABLE ts_device_event SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id'
);
SELECT add_compression_policy('ts_device_event', INTERVAL '7 days');

ALTER TABLE ts_device_session SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id'
);
SELECT add_compression_policy('ts_device_session', INTERVAL '7 days');

ALTER TABLE ts_device_command SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id'
);
SELECT add_compression_policy('ts_device_command', INTERVAL '7 days');

ALTER TABLE ts_app_log SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'level'
);
SELECT add_compression_policy('ts_app_log', INTERVAL '3 days');
