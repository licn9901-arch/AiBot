package com.deskpet.ai.tool;

import com.deskpet.ai.service.CoreApiClient;
import com.deskpet.ai.service.CoreApiClient.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 设备控制工具配置 - 注册 AI Tool Calling 所需的 Function Beans
 */
@Slf4j
@Configuration
public class DeviceToolsConfig {

    /**
     * 当前操作的设备ID（线程安全考虑，实际生产应使用 ThreadLocal 或 Context）
     */
    private static final ThreadLocal<String> currentDeviceId = new ThreadLocal<>();

    public static void setCurrentDeviceId(String deviceId) {
        currentDeviceId.set(deviceId);
    }

    public static String getCurrentDeviceId() {
        return currentDeviceId.get();
    }

    public static void clearCurrentDeviceId() {
        currentDeviceId.remove();
    }

    // ==================== Tool Request Records ====================

    public record MoveRequest(String direction, Double speed, Integer durationMs) {
    }

    public record StopRequest() {
    }

    public record EmotionRequest(String emotion, Integer durationMs) {
    }

    public record DeviceStateRequest() {
    }

    // ==================== Tool Beans ====================

    @Bean
    @Description("控制桌宠移动。direction: forward(前进)/backward(后退)/left(左转)/right(右转); speed: 速度0.0-1.0; durationMs: 持续时间毫秒")
    public Function<MoveRequest, String> sendMoveCommand(CoreApiClient coreApiClient) {
        return request -> {
            String deviceId = getCurrentDeviceId();
            if (deviceId == null) {
                return "错误：未指定设备ID";
            }

            // 安全限制
            double safeSpeed = Math.min(request.speed() != null ? request.speed() : 0.5, 0.8);
            int safeDuration = Math.min(request.durationMs() != null ? request.durationMs() : 1000, 3000);

            Map<String, Object> payload = new HashMap<>();
            payload.put("direction", request.direction());
            payload.put("speed", safeSpeed);
            payload.put("durationMs", safeDuration);

            log.info("Sending move command to {}: direction={}, speed={}, duration={}",
                    deviceId, request.direction(), safeSpeed, safeDuration);

            CommandResult result = coreApiClient.sendCommand(deviceId, "move", payload);
            if (result != null && !"FAILED".equals(result.status())) {
                return String.format("移动指令已发送，方向=%s，速度=%.1f，时长=%dms，请求ID=%s",
                        request.direction(), safeSpeed, safeDuration, result.reqId());
            } else {
                return "移动指令发送失败：" + (result != null ? result.ackMessage() : "未知错误");
            }
        };
    }

    @Bean
    @Description("停止桌宠移动")
    public Function<StopRequest, String> sendStopCommand(CoreApiClient coreApiClient) {
        return request -> {
            String deviceId = getCurrentDeviceId();
            if (deviceId == null) {
                return "错误：未指定设备ID";
            }

            log.info("Sending stop command to {}", deviceId);

            CommandResult result = coreApiClient.sendCommand(deviceId, "stop", Map.of());
            if (result != null && !"FAILED".equals(result.status())) {
                return "停止指令已发送，请求ID=" + result.reqId();
            } else {
                return "停止指令发送失败：" + (result != null ? result.ackMessage() : "未知错误");
            }
        };
    }

    @Bean
    @Description("设置桌宠表情。emotion: happy(开心)/sad(难过)/angry(生气)/sleepy(困倦)/idle(空闲); durationMs: 持续时间毫秒(可选)")
    public Function<EmotionRequest, String> setEmotion(CoreApiClient coreApiClient) {
        return request -> {
            String deviceId = getCurrentDeviceId();
            if (deviceId == null) {
                return "错误：未指定设备ID";
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("emotion", request.emotion());
            if (request.durationMs() != null) {
                payload.put("durationMs", Math.min(request.durationMs(), 5000));
            }

            log.info("Sending setEmotion command to {}: emotion={}", deviceId, request.emotion());

            CommandResult result = coreApiClient.sendCommand(deviceId, "setEmotion", payload);
            if (result != null && !"FAILED".equals(result.status())) {
                return String.format("表情指令已发送，表情=%s，请求ID=%s", request.emotion(), result.reqId());
            } else {
                return "表情指令发送失败：" + (result != null ? result.ackMessage() : "未知错误");
            }
        };
    }

    @Bean
    @Description("查询桌宠当前状态，包括是否在线、电量、信号强度等")
    public Function<DeviceStateRequest, String> getDeviceState(CoreApiClient coreApiClient) {
        return request -> {
            String deviceId = getCurrentDeviceId();
            if (deviceId == null) {
                return "错误：未指定设备ID";
            }

            log.info("Getting device state for {}", deviceId);

            var device = coreApiClient.getDevice(deviceId);
            if (device == null) {
                return "未找到设备：" + deviceId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("设备ID: ").append(device.deviceId()).append("\n");
            sb.append("在线状态: ").append(device.online() ? "在线" : "离线").append("\n");

            if (device.telemetry() != null) {
                var telemetry = device.telemetry();
                if (telemetry.containsKey("battery")) {
                    Object battery = telemetry.get("battery");
                    if (battery instanceof Number) {
                        sb.append("电量: ").append(String.format("%.0f%%", ((Number) battery).doubleValue() * 100)).append("\n");
                    }
                }
                if (telemetry.containsKey("rssi")) {
                    sb.append("信号强度: ").append(telemetry.get("rssi")).append(" dBm\n");
                }
                if (telemetry.containsKey("firmwareVersion")) {
                    sb.append("固件版本: ").append(telemetry.get("firmwareVersion")).append("\n");
                }
            }

            return sb.toString();
        };
    }
}
