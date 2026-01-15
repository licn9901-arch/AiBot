package com.deskpet.core.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record TelemetryRequest(
        int schemaVersion,
        long ts,
        String firmwareVersion,
        Integer rssi,
        Double battery,
        String lastAction,
        Map<String, Object> extra
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("schemaVersion", schemaVersion);
        map.put("ts", ts);
        if (firmwareVersion != null) {
            map.put("firmwareVersion", firmwareVersion);
        }
        if (rssi != null) {
            map.put("rssi", rssi);
        }
        if (battery != null) {
            map.put("battery", battery);
        }
        if (lastAction != null) {
            map.put("lastAction", lastAction);
        }
        if (extra != null && !extra.isEmpty()) {
            map.put("extra", extra);
        }
        return map;
    }
}
