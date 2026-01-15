package com.deskpet.gateway;

public record SendCommandResponse(
        boolean ok,
        String reason
) {
}
