-- ==================== V0.3 物模型管理 ====================

-- 产品/设备型号表（物模型挂载点）
CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    product_key     VARCHAR(50) NOT NULL UNIQUE,    -- 产品标识（如：deskpet-v1）
    name            VARCHAR(100) NOT NULL,           -- 产品名称
    description     VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DEPRECATED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_product_status ON product(status);

-- 物模型-属性定义
CREATE TABLE thing_model_property (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    identifier      VARCHAR(50) NOT NULL,            -- 属性标识（如：battery）
    name            VARCHAR(100) NOT NULL,           -- 属性名称（如：电池电量）
    data_type       VARCHAR(20) NOT NULL,            -- int / float / bool / string / enum / struct
    specs           JSONB,                           -- 数据规格（范围、枚举值等）
    access_mode     VARCHAR(10) NOT NULL DEFAULT 'r', -- r(只读) / rw(读写)
    required        BOOLEAN NOT NULL DEFAULT FALSE,
    description     VARCHAR(500),
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, identifier)
);

CREATE INDEX idx_thing_model_property_product ON thing_model_property(product_id);

-- 物模型-服务/指令定义
CREATE TABLE thing_model_service (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    identifier      VARCHAR(50) NOT NULL,            -- 服务标识（如：move）
    name            VARCHAR(100) NOT NULL,           -- 服务名称（如：移动控制）
    call_type       VARCHAR(20) NOT NULL DEFAULT 'async',  -- async(异步) / sync(同步)
    input_params    JSONB,                           -- 输入参数定义
    output_params   JSONB,                           -- 输出参数定义
    description     VARCHAR(500),
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, identifier)
);

CREATE INDEX idx_thing_model_service_product ON thing_model_service(product_id);

-- 物模型-事件定义
CREATE TABLE thing_model_event (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    identifier      VARCHAR(50) NOT NULL,            -- 事件标识（如：collision）
    name            VARCHAR(100) NOT NULL,           -- 事件名称（如：碰撞事件）
    event_type      VARCHAR(20) NOT NULL,            -- info / alert / error
    output_params   JSONB,                           -- 事件参数定义
    description     VARCHAR(500),
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, identifier)
);

CREATE INDEX idx_thing_model_event_product ON thing_model_event(product_id);

-- 设备事件记录表
CREATE TABLE device_event (
    id              BIGSERIAL PRIMARY KEY,
    device_id       VARCHAR(64) NOT NULL,
    event_id        VARCHAR(50) NOT NULL,            -- 事件标识
    event_type      VARCHAR(20) NOT NULL,            -- info / alert / error
    params          JSONB,                           -- 事件参数
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_event_device ON device_event(device_id, created_at DESC);
CREATE INDEX idx_device_event_type ON device_event(event_type, created_at DESC);
CREATE INDEX idx_device_event_id ON device_event(event_id, created_at DESC);

-- 设备表增加产品关联
ALTER TABLE device ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES product(id);
CREATE INDEX IF NOT EXISTS idx_device_product ON device(product_id);

-- ==================== 初始化默认产品与物模型 ====================

-- 创建默认产品
INSERT INTO product (product_key, name, description) VALUES
('deskpet-v1', '桌宠机器人V1', '第一代桌宠机器人，支持移动、表情、语音等功能');

-- 获取产品ID
DO $$
DECLARE
    v_product_id BIGINT;
BEGIN
    SELECT id INTO v_product_id FROM product WHERE product_key = 'deskpet-v1';

    -- 初始化属性定义
    INSERT INTO thing_model_property (product_id, identifier, name, data_type, specs, access_mode, required, description, sort_order) VALUES
    (v_product_id, 'battery', '电池电量', 'int', '{"min": 0, "max": 100, "unit": "%"}', 'r', true, '设备当前电池电量百分比', 1),
    (v_product_id, 'rssi', '信号强度', 'int', '{"min": -100, "max": 0, "unit": "dBm"}', 'r', false, 'WiFi信号强度', 2),
    (v_product_id, 'brightness', '屏幕亮度', 'int', '{"min": 0, "max": 100, "unit": "%"}', 'rw', false, '屏幕亮度设置', 3),
    (v_product_id, 'volume', '音量', 'int', '{"min": 0, "max": 100, "unit": "%"}', 'rw', false, '扬声器音量', 4),
    (v_product_id, 'version', '固件版本', 'string', '{"maxLength": 20}', 'r', false, '当前固件版本号', 5);

    -- 初始化服务定义
    INSERT INTO thing_model_service (product_id, identifier, name, call_type, input_params, output_params, description, sort_order) VALUES
    (v_product_id, 'move', '移动控制', 'async',
     '[{"identifier": "direction", "name": "方向", "dataType": "enum", "specs": {"values": ["forward", "backward", "left", "right", "stop"]}},
       {"identifier": "speed", "name": "速度", "dataType": "float", "specs": {"min": 0, "max": 1.0}},
       {"identifier": "durationMs", "name": "持续时间", "dataType": "int", "specs": {"min": 0, "max": 5000, "unit": "ms"}}]',
     '[]', '控制桌宠移动', 1),
    (v_product_id, 'setEmotion', '设置表情', 'async',
     '[{"identifier": "emotion", "name": "表情", "dataType": "enum", "specs": {"values": ["happy", "sad", "angry", "sleepy", "idle", "excited", "confused"]}}]',
     '[]', '设置桌宠表情', 2),
    (v_product_id, 'speak', '语音播报', 'async',
     '[{"identifier": "text", "name": "文本", "dataType": "string", "specs": {"maxLength": 200}},
       {"identifier": "voice", "name": "音色", "dataType": "enum", "specs": {"values": ["default", "cute", "robot"]}}]',
     '[]', '播放语音', 3),
    (v_product_id, 'playAnimation', '播放动画', 'async',
     '[{"identifier": "animation", "name": "动画", "dataType": "enum", "specs": {"values": ["wave", "dance", "nod", "shake", "sleep", "wakeup"]}}]',
     '[]', '播放预设动画', 4),
    (v_product_id, 'setBrightness', '设置亮度', 'async',
     '[{"identifier": "brightness", "name": "亮度", "dataType": "int", "specs": {"min": 0, "max": 100, "unit": "%"}}]',
     '[]', '设置屏幕亮度', 5),
    (v_product_id, 'setVolume', '设置音量', 'async',
     '[{"identifier": "volume", "name": "音量", "dataType": "int", "specs": {"min": 0, "max": 100, "unit": "%"}}]',
     '[]', '设置扬声器音量', 6),
    (v_product_id, 'reboot', '重启设备', 'async',
     '[]', '[]', '重启设备', 99);

    -- 初始化事件定义
    INSERT INTO thing_model_event (product_id, identifier, name, event_type, output_params, description, sort_order) VALUES
    (v_product_id, 'collision', '碰撞事件', 'alert',
     '[{"identifier": "direction", "name": "碰撞方向", "dataType": "enum", "specs": {"values": ["front", "back", "left", "right"]}},
       {"identifier": "intensity", "name": "碰撞强度", "dataType": "int", "specs": {"min": 0, "max": 100}}]',
     '设备检测到碰撞', 1),
    (v_product_id, 'touch', '触摸事件', 'info',
     '[{"identifier": "position", "name": "触摸位置", "dataType": "enum", "specs": {"values": ["head", "back", "belly"]}},
       {"identifier": "duration", "name": "触摸时长", "dataType": "int", "specs": {"unit": "ms"}}]',
     '设备检测到触摸', 2),
    (v_product_id, 'lowBattery', '低电量告警', 'alert',
     '[{"identifier": "level", "name": "当前电量", "dataType": "int", "specs": {"unit": "%"}}]',
     '电池电量低于阈值', 3),
    (v_product_id, 'fall', '跌落事件', 'error',
     '[{"identifier": "height", "name": "跌落高度", "dataType": "float", "specs": {"unit": "cm"}}]',
     '设备检测到跌落', 4),
    (v_product_id, 'voiceWakeup', '语音唤醒', 'info',
     '[{"identifier": "keyword", "name": "唤醒词", "dataType": "string"},
       {"identifier": "confidence", "name": "置信度", "dataType": "float", "specs": {"min": 0, "max": 1.0}}]',
     '语音唤醒触发', 5),
    (v_product_id, 'buttonPress', '按键事件', 'info',
     '[{"identifier": "button", "name": "按键", "dataType": "enum", "specs": {"values": ["power", "mode", "volume_up", "volume_down"]}},
       {"identifier": "pressType", "name": "按压类型", "dataType": "enum", "specs": {"values": ["short", "long", "double"]}}]',
     '物理按键被按下', 6);

END $$;
