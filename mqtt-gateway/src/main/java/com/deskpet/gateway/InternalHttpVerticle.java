package com.deskpet.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class InternalHttpVerticle extends AbstractVerticle {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String FIELD_REASON = "reason";
    private static final int COMMAND_TIMEOUT_MS = 3000;

    private GatewayConfig config;
    private ObjectMapper objectMapper;

    @Override
    public void start(Promise<Void> startPromise) {
        GatewayConfig.load(vertx).onComplete(ar -> {
            if (ar.failed()) {
                startPromise.fail(ar.cause());
                return;
            }
            this.config = ar.result();
            this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            router.post("/internal/command/send").handler(this::handleSendCommand);

            HttpServerOptions options = new HttpServerOptions();
            options.setReusePort(true);
            vertx.createHttpServer(options)
                    .requestHandler(router)
                    .listen(config.internalPort(), res -> {
                        if (res.succeeded()) {
                            log.info("Internal HTTP server started: port={} deploymentId={}", config.internalPort(), deploymentID());
                            startPromise.complete();
                        } else {
                            startPromise.fail(res.cause());
                        }
                    });
        });
    }

    private void handleSendCommand(RoutingContext ctx) {
        if (!isAuthorized(ctx)) {
            return;
        }
        SendCommandRequest request = parseRequest(ctx);
        if (request == null) {
            return;
        }
        if (request.payload() == null || request.payload().isBlank()) {
            sendJson(ctx, 400, false, "EMPTY_PAYLOAD");
            return;
        }

        String address = resolveRouteAddress(request.deviceId());
        if (address == null) {
            sendJson(ctx, 409, false, "OFFLINE");
            return;
        }

        JsonObject command = buildCommand(request);
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(COMMAND_TIMEOUT_MS);
        vertx.eventBus().request(address, command, options, ar -> handleCommandDispatch(ctx, ar));
    }

    private boolean isAuthorized(RoutingContext ctx) {
        if (config.internalToken().isBlank()) {
            return true;
        }
        String token = ctx.request().getHeader(HEADER_INTERNAL_TOKEN);
        if (config.internalToken().equals(token)) {
            return true;
        }
        sendJson(ctx, 401, false, "INVALID_INTERNAL_TOKEN");
        return false;
    }

    private SendCommandRequest parseRequest(RoutingContext ctx) {
        String body = ctx.body().asString(StandardCharsets.UTF_8.name());
        try {
            return objectMapper.readValue(body, SendCommandRequest.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            sendJson(ctx, 400, false, "BAD_REQUEST");
            return null;
        }
    }

    private String resolveRouteAddress(String deviceId) {
        LocalMap<String, String> routing = vertx.sharedData().getLocalMap(GatewayApplication.ROUTE_MAP_NAME);
        return routing.get(deviceId);
    }

    private JsonObject buildCommand(SendCommandRequest request) {
        int qos = normalizeQos(request.qos());
        return new JsonObject()
                .put("deviceId", request.deviceId())
                .put("topic", request.topic())
                .put("payload", request.payload())
                .put("qos", qos);
    }

    private int normalizeQos(int qos) {
        if (qos >= 0 && qos <= 2) {
            return qos;
        }
        return MqttQoS.AT_LEAST_ONCE.value();
    }

    private void handleCommandDispatch(RoutingContext ctx, AsyncResult<Message<Object>> ar) {
        if (ar.failed()) {
            log.warn("Command dispatch failed: {}", ar.cause().getMessage());
            sendJson(ctx, 500, false, "DISPATCH_FAILED");
            return;
        }
        JsonObject reply = (JsonObject) ar.result().body();
        boolean ok = reply.getBoolean("ok", false);
        String reason = reply.getString(FIELD_REASON, "");
        if (!ok) {
            sendJson(ctx, 409, false, reason);
            return;
        }
        sendJson(ctx, 200, true, "SENT");
    }

    private void sendJson(RoutingContext ctx, int statusCode, boolean ok, String reason) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                .end(new JsonObject().put("ok", ok).put(FIELD_REASON, reason).encode());
    }
}
