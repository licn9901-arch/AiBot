package com.deskpet.core.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * TimescaleDB 时序库数据源配置
 * 通过 timescaledb.enabled=true 启用
 */
@Configuration
@ConditionalOnProperty(name = "timescaledb.enabled", havingValue = "true")
public class TimescaleDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "timescaledb.datasource")
    public DataSource timescaleDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate timescaleJdbcTemplate(@Qualifier("timescaleDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
