package com.deskpet.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceSnGeneratorTest {

    private JdbcTemplate jdbcTemplate;
    private DeviceSnGenerator generator;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        generator = new DeviceSnGenerator(jdbcTemplate);
    }

    @Test
    void nextSn_returnsFormattedSn() {
        when(jdbcTemplate.queryForObject(eq(DeviceSnGenerator.NEXT_SN_SQL), eq(Long.class)))
                .thenReturn(12L);

        String sn = generator.nextSn("deskpet-v1");

        assertThat(sn).isEqualTo("deskpet-v1-000012");
    }

    @Test
    void nextSn_createsSequenceAndRetriesWhenMissing() {
        when(jdbcTemplate.queryForObject(eq(DeviceSnGenerator.NEXT_SN_SQL), eq(Long.class)))
                .thenThrow(new BadSqlGrammarException("nextval", DeviceSnGenerator.NEXT_SN_SQL, new SQLException("missing")))
                .thenReturn(1L);

        String sn = generator.nextSn("deskpet-v1");

        verify(jdbcTemplate).execute(DeviceSnGenerator.ENSURE_SEQUENCE_SQL);
        assertThat(sn).isEqualTo("deskpet-v1-000001");
    }
}
