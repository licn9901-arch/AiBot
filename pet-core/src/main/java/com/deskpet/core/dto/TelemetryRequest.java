package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;

public record TelemetryRequest(
        @Schema(description = "协议版本", example = "1")
        int schemaVersion,
        @Schema(description = "上报时间戳（秒）", example = "1730000000")
        long ts,
        @Schema(description = "固件版本", example = "0.1.0")
        String firmwareVersion,
        @Schema(description = "信号强度（dBm）", example = "-55")
        Integer rssi,
        @Schema(description = "电量百分比", example = "87")
        Double battery,
        @Schema(description = "最近一次动作/指令", example = "move")
        String lastAction,
        @Schema(description = "扩展字段", example = "{\"temperature\":26.5}")
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
