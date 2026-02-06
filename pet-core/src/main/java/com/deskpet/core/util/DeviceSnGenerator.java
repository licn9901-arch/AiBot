package com.deskpet.core.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 设备 SN 生成器
 * 格式：{productKey}-{6位序列号}，如 deskpet-v1-000001
 */
@Component
@RequiredArgsConstructor
public class DeviceSnGenerator {

    private final JdbcTemplate jdbcTemplate;

    public String nextSn(String productKey) {
        Long seq = jdbcTemplate.queryForObject("SELECT nextval('device_sn_seq')", Long.class);
        return String.format("%s-%06d", productKey, seq);
    }
}
