package com.deskpet.core.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    @NotBlank
    private String frontendBaseUrl;

    @NotBlank
    private String mailFrom;

    private Duration activationTokenTtl = Duration.ofHours(24);

    private Duration passwordResetTokenTtl = Duration.ofMinutes(30);
}
