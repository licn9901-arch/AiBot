package com.deskpet.gateway;

final class TopicAcl {
    private TopicAcl() {
    }

    static boolean isValidSubscribe(String deviceId, String topic) {
        return topic.equals("pet/" + deviceId + "/cmd")
                || topic.equals("pet/" + deviceId + "/resp");
    }

    static boolean isValidPublish(String deviceId, String topic) {
        return topic.equals("pet/" + deviceId + "/telemetry")
                || topic.equals("pet/" + deviceId + "/cmd/ack")
                || topic.equals("pet/" + deviceId + "/event")
                || topic.equals("pet/" + deviceId + "/req");
    }

    static boolean isValidDownlink(String deviceId, String topic) {
        return topic.equals("pet/" + deviceId + "/cmd")
                || topic.equals("pet/" + deviceId + "/resp");
    }

    static String resolveCallbackPath(String topic) {
        if (topic.endsWith("/telemetry")) {
            return "/internal/telemetry/";
        }
        if (topic.endsWith("/event")) {
            return "/internal/event/";
        }
        if (topic.endsWith("/req")) {
            return "/internal/request/";
        }
        return "/internal/ack/";
    }
}
