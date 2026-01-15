package com.deskpet.core.dto;

public record GatewaySendCommandResponse(
        boolean ok,
        String reason
) {
}
