CREATE TABLE device (
    device_id TEXT PRIMARY KEY,
    secret_hash TEXT NOT NULL,
    secret_salt TEXT NOT NULL,
    model TEXT,
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE device_session (
    device_id TEXT PRIMARY KEY REFERENCES device(device_id) ON DELETE CASCADE,
    online BOOLEAN NOT NULL,
    gateway_instance_id TEXT,
    ip TEXT,
    last_seen TIMESTAMPTZ NOT NULL
);

CREATE TABLE device_command (
    req_id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL REFERENCES device(device_id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    payload JSONB,
    status VARCHAR(32) NOT NULL,
    ack_code TEXT,
    ack_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_device_command_device_id ON device_command(device_id);
CREATE INDEX idx_device_command_status_updated ON device_command(status, updated_at);

CREATE TABLE telemetry_latest (
    device_id TEXT PRIMARY KEY REFERENCES device(device_id) ON DELETE CASCADE,
    telemetry JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE telemetry_history (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL REFERENCES device(device_id) ON DELETE CASCADE,
    telemetry JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_telemetry_history_device_id ON telemetry_history(device_id);
