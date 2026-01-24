package com.deskpet.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CoreClientConfig {

    @Value("${pet-core.base-url:http://localhost:8080}")
    private String coreBaseUrl;

    @Bean
    public WebClient coreWebClient() {
        return WebClient.builder()
                .baseUrl(coreBaseUrl)
                .build();
    }
}
