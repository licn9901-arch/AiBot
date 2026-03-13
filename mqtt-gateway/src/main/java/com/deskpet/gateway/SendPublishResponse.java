package com.deskpet.gateway;

public record SendPublishResponse(
        boolean ok,
        String reason
) {
}
