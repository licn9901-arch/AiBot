package com.deskpet;

import com.deskpet.gateway.InternalHttpVerticle;
import com.deskpet.gateway.MqttServerVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayApplication {

    public static final String ROUTE_MAP_NAME = "gateway.device.routing";
    public static final String COMMAND_ADDRESS_PREFIX = "gateway.command.";

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        publishVerticle(vertx, MqttServerVerticle.class.getName());
        publishVerticle(vertx, InternalHttpVerticle.class.getName());
    }

    private static void publishVerticle(Vertx vertx, String verticleName) {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int instances = cores * 2;
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(instances)
                .setHa(true);
        vertx.deployVerticle(verticleName, options, ar -> {
            if (ar.failed()) {
                log.error("Verticle deploy failed: {}", verticleName, ar.cause());
            }
        });
    }
}
