package com.deskpet.core.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 设备 SN 生成器
 * 格式：{productKey}-{6位序列号}，如 deskpet-v1-000001
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceSnGenerator {
    static final String NEXT_SN_SQL = "SELECT nextval('device_sn_seq')";
    static final String ENSURE_SEQUENCE_SQL =
            "CREATE SEQUENCE IF NOT EXISTS device_sn_seq START WITH 1 INCREMENT BY 1";

    private final JdbcTemplate jdbcTemplate;

    public String nextSn(String productKey) {
        Long seq;
        try {
            seq = queryNextSequenceValue();
        } catch (BadSqlGrammarException ex) {
            log.warn("device_sn_seq 不存在，尝试自动创建后重试: {}", ex.getMessage());
            ensureSequenceExists();
            seq = queryNextSequenceValue();
        }
        return String.format("%s-%06d", productKey, seq);
    }

    private Long queryNextSequenceValue() {
        return jdbcTemplate.queryForObject(NEXT_SN_SQL, Long.class);
    }

    private void ensureSequenceExists() {
        jdbcTemplate.execute(ENSURE_SEQUENCE_SQL);
    }
}
