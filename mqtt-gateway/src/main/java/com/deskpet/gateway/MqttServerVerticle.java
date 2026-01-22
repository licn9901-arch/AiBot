package com.deskpet.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.client.WebClient;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MqttServerVerticle extends AbstractVerticle {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final String UNKNOWN_IP = "unknown";

    private final Map<String, EndpointSession> sessions = new ConcurrentHashMap<>();
    private GatewayConfig config;
    private WebClient webClient;
    private ObjectMapper objectMapper;
    private String commandAddress;

    @Override
    public void start(Promise<Void> startPromise) {
        GatewayConfig.load(vertx).onComplete(ar -> {
            if (ar.failed()) {
                startPromise.fail(ar.cause());
                return;
            }
            this.config = ar.result();
            this.webClient = WebClient.create(vertx);
            this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            this.commandAddress = GatewayApplication.COMMAND_ADDRESS_PREFIX + deploymentID();

            vertx.eventBus().consumer(commandAddress, message -> {
                JsonObject body = (JsonObject) message.body();
                String deviceId = body.getString("deviceId");
                EndpointSession session = sessions.get(deviceId);
                if (session == null) {
                    message.reply(new JsonObject().put("ok", false).put("reason", "OFFLINE"));
                    return;
                }
                String topic = body.getString("topic");
                String payload = body.getString("payload", "");
                int qosValue = body.getInteger("qos", MqttQoS.AT_LEAST_ONCE.value());
                MqttQoS qos = MqttQoS.valueOf(qosValue);
                session.endpoint().publish(topic, Buffer.buffer(payload), qos, false, false);
                message.reply(new JsonObject().put("ok", true));
            });

            startMqttServer(startPromise);
        });
    }

    private void startMqttServer(Promise<Void> startPromise) {
        MqttServerOptions options = new MqttServerOptions()
                .setPort(config.mqttPort());
        options.setReusePort(GatewayApplication.isReusePortSupported());
        MqttServer.create(vertx, options)
                .endpointHandler(this::handleEndpoint)
                .listen(ar -> {
                    if (ar.succeeded()) {
                        log.info("MQTT server started: port={} deploymentId={}", config.mqttPort(), deploymentID());
                        startPromise.complete();
                    } else {
                        startPromise.fail(ar.cause());
                    }
                });
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
            authRequest.putHeader(HEADER_INTERNAL_TOKEN, config.internalToken());
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
                    String ip = remoteAddress == null ? UNKNOWN_IP : remoteAddress.host();
                    sessions.put(deviceId, new EndpointSession(deviceId, endpoint, Instant.now(), ip));
                    markRoute(deviceId);
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
            String ip = session == null ? UNKNOWN_IP : session.clientIp();
            clearRoute(deviceId);
            notifyPresence(deviceId, ip, false);
        });

        endpoint.closeHandler(v -> {
            EndpointSession session = sessions.remove(deviceId);
            String ip = session == null ? UNKNOWN_IP : session.clientIp();
            clearRoute(deviceId);
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
            postRequest.putHeader(HEADER_INTERNAL_TOKEN, config.internalToken());
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
                presenceRequest.putHeader(HEADER_INTERNAL_TOKEN, config.internalToken());
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

    private void markRoute(String deviceId) {
        LocalMap<String, String> routing = vertx.sharedData().getLocalMap(GatewayApplication.ROUTE_MAP_NAME);
        routing.put(deviceId, commandAddress);
    }

    private void clearRoute(String deviceId) {
        LocalMap<String, String> routing = vertx.sharedData().getLocalMap(GatewayApplication.ROUTE_MAP_NAME);
        String owner = routing.get(deviceId);
        if (commandAddress.equals(owner)) {
            routing.remove(deviceId);
        }
    }
}
