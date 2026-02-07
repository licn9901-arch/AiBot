package com.deskpet.core.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * TimescaleDB 时序库数据源配置
 * 通过 timescaledb.enabled=true 启用
 *
 * 当注册了额外的 DataSource bean 后，Spring Boot 不会再自动创建主数据源，
 * 因此必须在此处显式声明主数据源并标记为 @Primary。
 */
@Configuration
@ConditionalOnProperty(name = "timescaledb.enabled", havingValue = "true")
public class TimescaleDbConfig {

    // ===== 主数据源（@Primary，供 JPA / Flyway 使用）=====

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    // ===== TimescaleDB 数据源（仅供 TimeSeriesService 使用）=====

    @Bean("timescaleDataSourceProperties")
    @ConfigurationProperties(prefix = "timescaledb.datasource")
    public DataSourceProperties timescaleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("timescaleDataSource")
    public DataSource timescaleDataSource(
            @Qualifier("timescaleDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean("timescaleJdbcTemplate")
    public JdbcTemplate timescaleJdbcTemplate(
            @Qualifier("timescaleDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
