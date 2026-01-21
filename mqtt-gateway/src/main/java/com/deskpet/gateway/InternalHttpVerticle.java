package com.deskpet.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class InternalHttpVerticle extends AbstractVerticle {

    private GatewayConfig config;
    private ObjectMapper objectMapper;

    @Override
    public void start(Promise<Void> startPromise) {
        this.config = GatewayConfig.fromEnv();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
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
                if (request.payload() == null || request.payload().isBlank()) {
                    ctx.response().setStatusCode(400)
                            .putHeader("Content-Type", "application/json")
                            .end(objectMapper.writeValueAsString(new SendCommandResponse(false, "EMPTY_PAYLOAD")));
                    return;
                }

                LocalMap<String, String> routing = vertx.sharedData().getLocalMap(GatewayApplication.ROUTE_MAP_NAME);
                String address = routing.get(request.deviceId());
                if (address == null) {
                    ctx.response().setStatusCode(409)
                            .putHeader("Content-Type", "application/json")
                            .end(objectMapper.writeValueAsString(new SendCommandResponse(false, "OFFLINE")));
                    return;
                }

                int qos = request.qos() >= 0 && request.qos() <= 2 ? request.qos() : MqttQoS.AT_LEAST_ONCE.value();
                JsonObject command = new JsonObject()
                        .put("deviceId", request.deviceId())
                        .put("topic", request.topic())
                        .put("payload", request.payload())
                        .put("qos", qos);
                DeliveryOptions options = new DeliveryOptions().setSendTimeout(3000);
                vertx.eventBus().request(address, command, options, ar -> {
                    if (ar.failed()) {
                        log.warn("Command dispatch failed: {}", ar.cause().getMessage());
                        ctx.response().setStatusCode(500)
                                .putHeader("Content-Type", "application/json")
                                .end(new JsonObject().put("ok", false).put("reason", "DISPATCH_FAILED").encode());
                        return;
                    }
                    JsonObject reply = (JsonObject) ar.result().body();
                    boolean ok = reply.getBoolean("ok", false);
                    String reason = reply.getString("reason", "");
                    if (!ok) {
                        try {
                            ctx.response().setStatusCode(409)
                                    .putHeader("Content-Type", "application/json")
                                    .end(objectMapper.writeValueAsString(new SendCommandResponse(false, reason)));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }
                    try {
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(objectMapper.writeValueAsString(new SendCommandResponse(true, "SENT")));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                ctx.response().setStatusCode(400)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject().put("ok", false).put("reason", "BAD_REQUEST").encode());
            }
        });

        HttpServerOptions options = new HttpServerOptions();
        options.setReusePort(GatewayApplication.isReusePortSupported());
        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(config.internalPort(), ar -> {
                    if (ar.succeeded()) {
                        log.info("Internal HTTP server started: port={} deploymentId={}", config.internalPort(), deploymentID());
                        startPromise.complete();
                    } else {
                        startPromise.fail(ar.cause());
                    }
                });
    }
}
