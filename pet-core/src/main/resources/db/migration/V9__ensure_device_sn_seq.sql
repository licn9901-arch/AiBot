-- 兜底创建设备 SN 序列，修复旧环境缺失序列的问题
CREATE SEQUENCE IF NOT EXISTS device_sn_seq START WITH 1 INCREMENT BY 1;
