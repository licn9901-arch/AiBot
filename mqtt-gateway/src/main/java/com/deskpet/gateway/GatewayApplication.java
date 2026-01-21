package com.deskpet.gateway;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class GatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    static final String ROUTE_MAP_NAME = "gateway.device.routing";
    static final String COMMAND_ADDRESS_PREFIX = "gateway.command.";

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        publishVerticle(vertx, MqttServerVerticle.class.getName());
        publishVerticle(vertx, InternalHttpVerticle.class.getName());
    }

    private static void publishVerticle(Vertx vertx, String verticleName) {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        boolean reusePort = isReusePortSupported();
        int instances = reusePort ? cores * 2 : 1;
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(instances)
                .setHa(true);
        vertx.deployVerticle(verticleName, options, ar -> {
            if (ar.failed()) {
                log.error("Verticle deploy failed: {}", verticleName, ar.cause());
            }
        });
    }

    static boolean isReusePortSupported() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return !osName.contains("win");
    }
}
