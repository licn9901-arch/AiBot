package com.deskpet.gateway;

public record GatewayConfig(
        int mqttPort,
        int internalPort,
        String coreInternalBaseUrl,
        String instanceId,
        String internalToken
) {
    public static GatewayConfig fromEnv() {
        int mqttPort = Integer.parseInt(System.getProperty("mqtt.port", "1883"));
        int internalPort = Integer.parseInt(System.getProperty("internal.port", "8081"));
        String coreInternalBaseUrl = System.getProperty("core.internalBaseUrl", "http://localhost:8080");
        String instanceId = System.getProperty("gateway.instanceId", "gateway-1");
        String internalToken = System.getProperty("internal.token", "");
        return new GatewayConfig(mqttPort, internalPort, coreInternalBaseUrl, instanceId, internalToken);
    }
}
