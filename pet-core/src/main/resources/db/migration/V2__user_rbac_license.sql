-- ==================== V0.2 用户系统 + RBAC + 授权码 ====================

-- 用户表
CREATE TABLE sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(20),
    avatar          VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_status ON sys_user(status);

-- 角色表
CREATE TABLE sys_role (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 权限表
CREATE TABLE sys_permission (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 用户-角色关联表
CREATE TABLE sys_user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_sys_user_role_user ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role ON sys_user_role(role_id);

-- 角色-权限关联表
CREATE TABLE sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES sys_permission(id) ON DELETE CASCADE,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_sys_role_permission_role ON sys_role_permission(role_id);

-- 授权码表
CREATE TABLE license_code (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32) NOT NULL UNIQUE,
    batch_no        VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    device_id       VARCHAR(64) UNIQUE,
    user_id         BIGINT REFERENCES sys_user(id),
    activated_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    remark          VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_license_code_status ON license_code(status);
CREATE INDEX idx_license_code_user ON license_code(user_id);
CREATE INDEX idx_license_code_device ON license_code(device_id);
CREATE INDEX idx_license_code_batch ON license_code(batch_no);

-- 操作日志表
CREATE TABLE operation_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES sys_user(id),
    device_id       VARCHAR(64),
    action          VARCHAR(50) NOT NULL,
    payload         JSONB,
    ip              VARCHAR(50),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_operation_log_user ON operation_log(user_id, created_at DESC);
CREATE INDEX idx_operation_log_device ON operation_log(device_id, created_at DESC);
CREATE INDEX idx_operation_log_action ON operation_log(action, created_at DESC);

-- ==================== 初始化数据 ====================

-- 初始化角色
INSERT INTO sys_role (code, name, description) VALUES
('ADMIN', '系统管理员', '拥有所有权限'),
('USER', '普通用户', '可绑定设备、控制自己的设备'),
('OPERATOR', '运维人员', '可查看设备状态、日志，不可修改配置');

-- 初始化权限
INSERT INTO sys_permission (code, name) VALUES
-- 用户管理
('user:list', '用户列表'),
('user:create', '创建用户'),
('user:update', '更新用户'),
('user:delete', '删除用户'),
('user:disable', '禁用用户'),
-- 角色管理
('role:list', '角色列表'),
('role:create', '创建角色'),
('role:update', '更新角色'),
('role:delete', '删除角色'),
('role:assign', '分配角色'),
-- 授权码管理
('license:list', '授权码列表'),
('license:generate', '生成授权码'),
('license:revoke', '撤销授权码'),
('license:export', '导出授权码'),
('license:activate', '激活授权码'),
-- 设备管理
('device:list', '设备列表'),
('device:view', '查看设备'),
('device:control', '控制设备'),
('device:register', '注册设备'),
('device:delete', '删除设备'),
-- 产品/物模型
('product:list', '产品列表'),
('product:create', '创建产品'),
('product:update', '更新产品'),
('product:delete', '删除产品'),
-- 日志
('log:list', '日志列表'),
('log:export', '导出日志');

-- ADMIN 角色拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE code = 'ADMIN'),
    id
FROM sys_permission;

-- USER 角色权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE code = 'USER'),
    id
FROM sys_permission
WHERE code IN ('license:activate', 'device:view', 'device:control');

-- OPERATOR 角色权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT
    (SELECT id FROM sys_role WHERE code = 'OPERATOR'),
    id
FROM sys_permission
WHERE code IN ('user:list', 'license:list', 'device:list', 'device:view', 'product:list', 'log:list');

-- 创建默认管理员账号（密码：admin123，使用 BCrypt 加密）
-- 密码 hash 为 BCrypt 加密的 "admin123"
INSERT INTO sys_user (username, password_hash, status) VALUES
('admin', '$2a$10$EqKcp1WFKVQISheBxkV.qOXEHGBFjLNiSuLYwrMpfVs7YJLaqSHGC', 'ACTIVE');

-- 给管理员分配 ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT
    (SELECT id FROM sys_user WHERE username = 'admin'),
    (SELECT id FROM sys_role WHERE code = 'ADMIN');
