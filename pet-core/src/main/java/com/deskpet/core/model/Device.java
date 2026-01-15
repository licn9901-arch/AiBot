package com.deskpet.core.model;

import java.time.Instant;

public record Device(
        String deviceId,
        String secret,
        String model,
        String remark,
        Instant createdAt
) {
}
