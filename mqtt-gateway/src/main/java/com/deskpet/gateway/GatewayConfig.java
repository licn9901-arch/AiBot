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
        String internalToken,
        int authTimeoutMs,
        int authMaxRetries,
        int authRetryDelayMs,
        boolean authFailOpen,
        int callbackTimeoutMs,
        int callbackMaxRetries,
        int callbackRetryDelayMs,
        boolean metricsEnabled,
        String metricsPath,
        int statsLogIntervalSec
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
        JsonObject auth = config.getJsonObject("auth", new JsonObject());
        int authTimeoutMs = auth.getInteger("timeoutMs", 2000);
        int authMaxRetries = auth.getInteger("maxRetries", 1);
        int authRetryDelayMs = auth.getInteger("retryDelayMs", 200);
        boolean authFailOpen = auth.getBoolean("failOpen", false);
        JsonObject callback = config.getJsonObject("callback", new JsonObject());
        int callbackTimeoutMs = callback.getInteger("timeoutMs", 2000);
        int callbackMaxRetries = callback.getInteger("maxRetries", 1);
        int callbackRetryDelayMs = callback.getInteger("retryDelayMs", 200);
        JsonObject metrics = config.getJsonObject("metrics", new JsonObject());
        boolean metricsEnabled = metrics.getBoolean("enabled", false);
        String metricsPath = metrics.getString("path", "/metrics");
        JsonObject stats = config.getJsonObject("stats", new JsonObject());
        int statsLogIntervalSec = stats.getInteger("logIntervalSec", 60);
        return new GatewayConfig(mqttPort, internalPort, coreInternalBaseUrl, instanceId, internalToken,
                authTimeoutMs, authMaxRetries, authRetryDelayMs, authFailOpen,
                callbackTimeoutMs, callbackMaxRetries, callbackRetryDelayMs,
                metricsEnabled, metricsPath, statsLogIntervalSec);
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
