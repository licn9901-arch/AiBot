package com.deskpet.core.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "DeskPet Core API",
                version = "0.1.0",
                description = "DeskPet 对外 REST API（V0.1）"
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "本地开发")
        }
)
@Configuration
public class OpenApiConfig {
}
