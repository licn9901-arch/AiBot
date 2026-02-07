package com.deskpet.core.service;

import com.deskpet.core.dto.CommandResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket 推送服务
 * 负责向前端推送指令状态变更和设备上下线事件
 */
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送指令状态变更
     */
    public void pushCommandStatus(String deviceId, CommandResponse response) {
        messagingTemplate.convertAndSend(
                "/topic/device/" + deviceId + "/command-status", response);
    }

    /**
     * 推送设备上下线
     */
    public void pushPresence(String deviceId, boolean online) {
        messagingTemplate.convertAndSend(
                "/topic/device/" + deviceId + "/presence",
                Map.of("deviceId", deviceId, "online", online, "ts", Instant.now().toString()));
    }
}
