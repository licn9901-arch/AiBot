CREATE TABLE pending_device_secret (
    id          BIGSERIAL PRIMARY KEY,
    batch_no    VARCHAR(50) NOT NULL,
    device_id   VARCHAR(64) NOT NULL,
    code        VARCHAR(32) NOT NULL,
    raw_secret  VARCHAR(64) NOT NULL,
    product_key VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pending_secret_batch ON pending_device_secret(batch_no);
