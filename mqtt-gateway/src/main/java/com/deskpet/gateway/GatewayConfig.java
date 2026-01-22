package com.deskpet.gateway;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.net.URL;
import java.nio.file.Paths;

public record GatewayConfig(
        int mqttPort,
        int internalPort,
        String coreInternalBaseUrl,
        String instanceId,
        String internalToken
) {
    public static Future<GatewayConfig> load(Vertx vertx) {
        ConfigStoreOptions store = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", resolveConfigPath()));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(store));
        Promise<GatewayConfig> promise = Promise.promise();
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                promise.fail(ar.cause());
                return;
            }
            promise.complete(fromConfig(ar.result()));
        });
        return promise.future();
    }

    private static GatewayConfig fromConfig(JsonObject config) {
        JsonObject mqtt = config.getJsonObject("mqtt", new JsonObject());
        int mqttPort = mqtt.getInteger("port", 1883);
        JsonObject internal = config.getJsonObject("internal", new JsonObject());
        int internalPort = internal.getInteger("port", 8081);
        String internalToken = internal.getString("token", "");
        JsonObject core = config.getJsonObject("core", new JsonObject());
        String coreInternalBaseUrl = core.getString("internalBaseUrl", "http://localhost:8080");
        JsonObject gateway = config.getJsonObject("gateway", new JsonObject());
        String instanceId = gateway.getString("instanceId", "gateway-1");
        return new GatewayConfig(mqttPort, internalPort, coreInternalBaseUrl, instanceId, internalToken);
    }

    private static String resolveConfigPath() {
        String configured = System.getProperty("gateway.config");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        URL resource = GatewayConfig.class.getClassLoader().getResource("config.yaml");
        if (resource != null && "file".equalsIgnoreCase(resource.getProtocol())) {
            try {
                return Paths.get(resource.toURI()).toString();
            } catch (Exception ignored) {
                // 使用默认配置路径
            }
        }
        return "config.yaml";
    }
}
