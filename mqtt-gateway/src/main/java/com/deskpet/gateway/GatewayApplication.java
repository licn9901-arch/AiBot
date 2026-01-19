package com.deskpet.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    private final Vertx vertx;
    private final GatewayConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, EndpointSession> sessions = new ConcurrentHashMap<>();

    public GatewayApplication(Vertx vertx, GatewayConfig config) {
        this.vertx = vertx;
        this.config = config;
        this.webClient = WebClient.create(vertx);
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) {
        GatewayConfig config = GatewayConfig.fromEnv();
        GatewayApplication app = new GatewayApplication(Vertx.vertx(), config);
        app.start();
    }

    public void start() {
        startMqttServer();
        startInternalHttp();
        log.info("Gateway started: mqtt={} internal={}", config.mqttPort(), config.internalPort());
    }

    private void startMqttServer() {
        MqttServerOptions options = new MqttServerOptions().setPort(config.mqttPort());
        MqttServer.create(vertx, options).endpointHandler(this::handleEndpoint).listen();
    }

    private void handleEndpoint(MqttEndpoint endpoint) {
        String deviceId = endpoint.clientIdentifier();
        String username = endpoint.auth() != null ? endpoint.auth().getUsername() : null;
        String password = endpoint.auth() != null ? endpoint.auth().getPassword() : null;
        if (username == null || password == null || !deviceId.equals(username)) {
            endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
            return;
        }

        String authUrl = config.coreInternalBaseUrl() + "/internal/auth";
        var authRequest = webClient.getAbs(authUrl);
        if (!config.internalToken().isBlank()) {
            authRequest.putHeader("X-Internal-Token", config.internalToken());
        }
        authRequest
                .addQueryParam("deviceId", deviceId)
                .addQueryParam("secret", password)
                .send(ar -> {
                    if (ar.failed() || ar.result().statusCode() != 200) {
                        endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                        return;
                    }
                    endpoint.accept(false);
                    SocketAddress remoteAddress = endpoint.remoteAddress();
                    String ip = remoteAddress == null ? "unknown" : remoteAddress.host();
                    sessions.put(deviceId, new EndpointSession(deviceId, endpoint, Instant.now(), ip));
                    notifyPresence(deviceId, ip, true);
                    wireEndpointHandlers(endpoint, deviceId);
                });
    }

    private void wireEndpointHandlers(MqttEndpoint endpoint, String deviceId) {
        endpoint.subscribeHandler(subscribe -> {
            var suback = subscribe.topicSubscriptions().stream()
                    .map(sub -> isValidSubscribe(deviceId, sub.topicName()) ? sub.qualityOfService() : MqttQoS.FAILURE)
                    .toList();
            endpoint.subscribeAcknowledge(subscribe.messageId(), suback);
        });

        endpoint.publishHandler(message -> handlePublish(deviceId, message));

        endpoint.disconnectHandler(v -> {
            EndpointSession session = sessions.remove(deviceId);
            String ip = session == null ? "unknown" : session.clientIp();
            notifyPresence(deviceId, ip, false);
        });

        endpoint.closeHandler(v -> {
            EndpointSession session = sessions.remove(deviceId);
            String ip = session == null ? "unknown" : session.clientIp();
            notifyPresence(deviceId, ip, false);
        });
    }

    private void handlePublish(String deviceId, MqttPublishMessage message) {
        String topic = message.topicName();
        if (!isValidPublish(deviceId, topic)) {
            return;
        }
        String targetPath = topic.endsWith("/telemetry") ? "/internal/telemetry/" : "/internal/ack/";
        String url = config.coreInternalBaseUrl() + targetPath + deviceId;
        Buffer payload = message.payload();
        var postRequest = webClient.postAbs(url);
        if (!config.internalToken().isBlank()) {
            postRequest.putHeader("X-Internal-Token", config.internalToken());
        }
        postRequest
                .sendBuffer(payload, ar -> {
                    if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                        EndpointSession session = sessions.get(deviceId);
                        if (session != null) {
                            session.endpoint().publishAcknowledge(message.messageId());
                        }
                    }
                });
    }

    private boolean isValidSubscribe(String deviceId, String topic) {
        return topic.equals("pet/" + deviceId + "/cmd");
    }

    private boolean isValidPublish(String deviceId, String topic) {
        return topic.equals("pet/" + deviceId + "/telemetry") || topic.equals("pet/" + deviceId + "/cmd/ack");
    }

    private void notifyPresence(String deviceId, String ip, boolean online) {
        String url = config.coreInternalBaseUrl() + (online ? "/internal/gateway/deviceOnline" : "/internal/gateway/deviceOffline");
        GatewayPresenceRequest request = new GatewayPresenceRequest(deviceId, config.instanceId(), ip);
        try {
            String body = objectMapper.writeValueAsString(request);
            var presenceRequest = webClient.postAbs(url);
            if (!config.internalToken().isBlank()) {
                presenceRequest.putHeader("X-Internal-Token", config.internalToken());
            }
            presenceRequest
                    .putHeader("Content-Type", "application/json")
                    .sendBuffer(Buffer.buffer(body), ar -> {
                        if (ar.failed()) {
                            log.warn("Presence notify failed for {}", deviceId);
                        }
                    });
        } catch (Exception e) {
            log.error("Presence notify error: {}", e.getMessage(), e);
        }
    }

    private void startInternalHttp() {
        Router router = Router.router(vertx);
        router.route().handler(io.vertx.ext.web.handler.BodyHandler.create());
        router.post("/internal/command/send").handler(ctx -> {
            if (!config.internalToken().isBlank()) {
                String token = ctx.request().getHeader("X-Internal-Token");
                if (!config.internalToken().equals(token)) {
                    ctx.response().setStatusCode(401)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("ok", false).put("reason", "INVALID_INTERNAL_TOKEN").encode());
                    return;
                }
            }
            String body = ctx.body().asString(StandardCharsets.UTF_8.name());
            try {
                SendCommandRequest request = objectMapper.readValue(body, SendCommandRequest.class);
                EndpointSession session = sessions.get(request.deviceId());
                if (session == null) {
                    ctx.response().setStatusCode(409)
                            .putHeader("Content-Type", "application/json")
                            .end(objectMapper.writeValueAsString(new SendCommandResponse(false, "OFFLINE")));
                    return;
                }
                String payload = request.payload();
                if (payload == null || payload.isBlank()) {
                    ctx.response().setStatusCode(400)
                            .putHeader("Content-Type", "application/json")
                            .end(objectMapper.writeValueAsString(new SendCommandResponse(false, "EMPTY_PAYLOAD")));
                    return;
                }
                MqttQoS qos = request.qos() >= 0 && request.qos() <= 2 ? MqttQoS.valueOf(request.qos()) : MqttQoS.AT_LEAST_ONCE;
                session.endpoint().publish(request.topic(), Buffer.buffer(payload), qos, false, false);
                ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(objectMapper.writeValueAsString(new SendCommandResponse(true, "SENT")));
            } catch (Exception e) {
                ctx.response().setStatusCode(400)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject().put("ok", false).put("reason", "BAD_REQUEST").encode());
            }
        });

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(config.internalPort());
    }
}
