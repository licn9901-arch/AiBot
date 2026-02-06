package com.deskpet.gateway;

import com.deskpet.GatewayApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class MqttServerVerticle extends AbstractVerticle {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final String UNKNOWN_IP = "unknown";

    private final Map<String, EndpointSession> sessions = new ConcurrentHashMap<>();
    private GatewayConfig config;
    private WebClient webClient;
    private ObjectMapper objectMapper;
    private String commandAddress;
    private GatewayMetrics metrics;

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
            this.metrics = GatewayMetrics.getInstance();

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

            scheduleStatsLog(vertx);
            startMqttServer(startPromise);
        });
    }

    private void startMqttServer(Promise<Void> startPromise) {
        MqttServerOptions options = new MqttServerOptions()
                .setPort(config.mqttPort());
        options.setReusePort(true);
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
        Supplier<HttpRequest<Buffer>> requestSupplier = () -> {
            HttpRequest<Buffer> authRequest = webClient.getAbs(authUrl);
            if (!config.internalToken().isBlank()) {
                authRequest.putHeader(HEADER_INTERNAL_TOKEN, config.internalToken());
            }
            return authRequest
                    .addQueryParam("deviceId", deviceId)
                    .addQueryParam("secret", password);
        };

        sendWithRetry(requestSupplier, null, config.authTimeoutMs(), config.authMaxRetries(),
                config.authRetryDelayMs(), () -> metrics.onAuthRetry(), ar -> {
                    if (isAuthOk(ar)) {
                        acceptEndpoint(endpoint, deviceId);
                        return;
                    }
                    boolean failOpen = config.authFailOpen() && shouldFailOpen(ar);
                    if (failOpen) {
                        log.warn("Auth failed but fail-open enabled: deviceId={}", deviceId);
                        acceptEndpoint(endpoint, deviceId);
                        return;
                    }
                    metrics.onAuthFail();
                    log.warn("Auth rejected: deviceId={} status={}", deviceId, statusOf(ar));
                    endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
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

        endpoint.disconnectHandler(v -> handleDisconnect(deviceId));

        endpoint.closeHandler(v -> handleDisconnect(deviceId));
    }

    private void handlePublish(String deviceId, MqttPublishMessage message) {
        String topic = message.topicName();
        if (!isValidPublish(deviceId, topic)) {
            log.warn("Publish denied: deviceId={} topic={}", deviceId, topic);
            return;
        }
        boolean telemetry = topic.endsWith("/telemetry");
        boolean event = topic.endsWith("/event");
        String targetPath;
        if (telemetry) {
            targetPath = "/internal/telemetry/";
        } else if (event) {
            targetPath = "/internal/event/";
        } else {
            targetPath = "/internal/ack/";
        }
        String url = config.coreInternalBaseUrl() + targetPath + deviceId;
        Buffer payload = message.payload();
        long count;
        if (telemetry) {
            count = metrics.onTelemetry();
        } else if (event) {
            count = metrics.onEvent();
        } else {
            count = metrics.onAck();
        }
        if (log.isDebugEnabled()) {
            String type = telemetry ? "telemetry" : (event ? "event" : "ack");
            log.debug("Upstream {} received: deviceId={} count={}", type, deviceId, count);
        }
        Supplier<HttpRequest<Buffer>> requestSupplier = () -> {
            HttpRequest<Buffer> postRequest = webClient.postAbs(url);
            if (!config.internalToken().isBlank()) {
                postRequest.putHeader(HEADER_INTERNAL_TOKEN, config.internalToken());
            }
            postRequest.putHeader("Content-Type", "application/json");
            return postRequest;
        };
        sendWithRetry(requestSupplier, payload, config.callbackTimeoutMs(), config.callbackMaxRetries(),
                config.callbackRetryDelayMs(), null, ar -> {
                    if (ar.failed() || (ar.result() != null && ar.result().statusCode() >= 400)) {
                        metrics.onCallbackFail();
                        log.warn("Callback failed: deviceId={} topic={} status={}", deviceId, topic, statusOf(ar));
                    }
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
        return topic.equals("pet/" + deviceId + "/telemetry")
            || topic.equals("pet/" + deviceId + "/cmd/ack")
            || topic.equals("pet/" + deviceId + "/event");
    }

    private void notifyPresence(String deviceId, String ip, boolean online) {
        String url = config.coreInternalBaseUrl() + (online ? "/internal/gateway/deviceOnline" : "/internal/gateway/deviceOffline");
        GatewayPresenceRequest request = new GatewayPresenceRequest(deviceId, config.instanceId(), ip);
        try {
            String body = objectMapper.writeValueAsString(request);
            Supplier<HttpRequest<Buffer>> requestSupplier = () -> {
                HttpRequest<Buffer> presenceRequest = webClient.postAbs(url);
                if (!config.internalToken().isBlank()) {
                    presenceRequest.putHeader(HEADER_INTERNAL_TOKEN, config.internalToken());
                }
                presenceRequest.putHeader("Content-Type", "application/json");
                return presenceRequest;
            };
            sendWithRetry(requestSupplier, Buffer.buffer(body), config.callbackTimeoutMs(),
                    config.callbackMaxRetries(), config.callbackRetryDelayMs(), null, ar -> {
                        if (ar.failed() || (ar.result() != null && ar.result().statusCode() >= 400)) {
                            metrics.onCallbackFail();
                            log.warn("Presence notify failed: deviceId={} status={}", deviceId, statusOf(ar));
                        }
                    });
        } catch (Exception e) {
            log.error("Presence notify error: {}", e.getMessage(), e);
        }
    }

    private void markRoute(String deviceId) {
        LocalMap<String, String> routing = vertx.sharedData().getLocalMap(GatewayApplication.ROUTE_MAP_NAME);
        routing.put(deviceId, commandAddress);
        metrics.setOnlineCount(routing.size());
    }

    private void clearRoute(String deviceId) {
        LocalMap<String, String> routing = vertx.sharedData().getLocalMap(GatewayApplication.ROUTE_MAP_NAME);
        String owner = routing.get(deviceId);
        if (commandAddress.equals(owner)) {
            routing.remove(deviceId);
        }
        metrics.setOnlineCount(routing.size());
    }

    private void acceptEndpoint(MqttEndpoint endpoint, String deviceId) {
        endpoint.accept(false);
        SocketAddress remoteAddress = endpoint.remoteAddress();
        String ip = remoteAddress == null ? UNKNOWN_IP : remoteAddress.host();
        sessions.put(deviceId, new EndpointSession(deviceId, endpoint, Instant.now(), ip));
        markRoute(deviceId);
        metrics.onConnect();
        log.info("Device connected: deviceId={} ip={} online={}", deviceId, ip, metrics.onlineCount());
        notifyPresence(deviceId, ip, true);
        wireEndpointHandlers(endpoint, deviceId);
    }

    private void handleDisconnect(String deviceId) {
        EndpointSession session = sessions.remove(deviceId);
        if (session == null) {
            return;
        }
        String ip = session.clientIp();
        clearRoute(deviceId);
        metrics.onDisconnect();
        log.info("Device disconnected: deviceId={} ip={} online={}", deviceId, ip, metrics.onlineCount());
        notifyPresence(deviceId, ip, false);
    }

    private void scheduleStatsLog(Vertx vertx) {
        int intervalSec = config.statsLogIntervalSec();
        if (intervalSec <= 0) {
            return;
        }
        vertx.setPeriodic(intervalSec * 1000L, id -> log.info(
                "Gateway stats: online={} telemetry={} ack={} cmdSend={} connect={} disconnect={} authFail={} callbackFail={}",
                metrics.onlineCount(),
                metrics.telemetryCount(),
                metrics.ackCount(),
                metrics.commandSendCount(),
                metrics.connectCount(),
                metrics.disconnectCount(),
                metrics.authFailCount(),
                metrics.callbackFailCount()));
    }

    private void sendWithRetry(Supplier<HttpRequest<Buffer>> requestSupplier,
                               Buffer payload,
                               int timeoutMs,
                               int maxRetries,
                               int retryDelayMs,
                               Runnable onRetry,
                               io.vertx.core.Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        attemptSend(requestSupplier, payload, timeoutMs, maxRetries, retryDelayMs, 0, onRetry, handler);
    }

    private void attemptSend(Supplier<HttpRequest<Buffer>> requestSupplier,
                             Buffer payload,
                             int timeoutMs,
                             int maxRetries,
                             int retryDelayMs,
                             int attempt,
                             Runnable onRetry,
                             io.vertx.core.Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
        HttpRequest<Buffer> request = requestSupplier.get();
        if (timeoutMs > 0) {
            request.timeout(timeoutMs);
        }
        io.vertx.core.Handler<AsyncResult<HttpResponse<Buffer>>> wrapped = ar -> {
            if (shouldRetry(ar) && attempt < maxRetries) {
                if (onRetry != null) {
                    onRetry.run();
                }
                long delay = Math.max(0, retryDelayMs);
                if (delay == 0) {
                    attemptSend(requestSupplier, payload, timeoutMs, maxRetries, retryDelayMs, attempt + 1, onRetry, handler);
                } else {
                    vertx.setTimer(delay, id -> attemptSend(requestSupplier, payload, timeoutMs, maxRetries,
                            retryDelayMs, attempt + 1, onRetry, handler));
                }
                return;
            }
            handler.handle(ar);
        };
        if (payload == null) {
            request.send(wrapped);
        } else {
            request.sendBuffer(payload, wrapped);
        }
    }

    private boolean shouldRetry(AsyncResult<HttpResponse<Buffer>> ar) {
        if (ar.failed()) {
            return true;
        }
        return ar.result().statusCode() >= 500;
    }

    private boolean isAuthOk(AsyncResult<HttpResponse<Buffer>> ar) {
        return ar.succeeded() && ar.result().statusCode() == 200;
    }

    private boolean shouldFailOpen(AsyncResult<HttpResponse<Buffer>> ar) {
        if (ar.failed()) {
            return true;
        }
        return ar.result().statusCode() >= 500;
    }

    private int statusOf(AsyncResult<HttpResponse<Buffer>> ar) {
        if (ar == null) {
            return -1;
        }
        if (ar.succeeded() && ar.result() != null) {
            return ar.result().statusCode();
        }
        return -1;
    }
}
