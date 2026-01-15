package com.deskpet.core.config;

import com.deskpet.core.store.CommandStore;
import com.deskpet.core.store.DeviceStore;
import com.deskpet.core.store.SessionStore;
import com.deskpet.core.store.TelemetryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {
    @Bean
    public DeviceStore deviceStore() {
        return new DeviceStore();
    }

    @Bean
    public SessionStore sessionStore() {
        return new SessionStore();
    }

    @Bean
    public CommandStore commandStore() {
        return new CommandStore();
    }

    @Bean
    public TelemetryStore telemetryStore() {
        return new TelemetryStore();
    }
}
