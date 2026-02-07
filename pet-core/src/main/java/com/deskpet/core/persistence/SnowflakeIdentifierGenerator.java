package com.deskpet.core.persistence;

import com.deskpet.core.util.SnowflakeIdGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Hibernate ID 生成器，委托给 Spring 管理的 {@link SnowflakeIdGenerator}
 */
public class SnowflakeIdentifierGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return ApplicationContextHolder.getBean(SnowflakeIdGenerator.class).nextId();
    }

    /**
     * 静态持有 ApplicationContext，供非 Spring 管理的 Hibernate 组件获取 Bean
     */
    @Component
    public static class ApplicationContextHolder implements ApplicationContextAware {

        private static ApplicationContext context;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            context = applicationContext;
        }

        public static <T> T getBean(Class<T> clazz) {
            return context.getBean(clazz);
        }
    }
}
