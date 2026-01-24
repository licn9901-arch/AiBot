package com.deskpet.ai.controller;

import com.deskpet.ai.dto.ChatRequest;
import com.deskpet.ai.dto.ChatResponse;
import com.deskpet.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "对话接口", description = "AI 对话控制接口")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    @Operation(summary = "发送对话", description = "向 AI 发送消息，AI 会理解意图并控制设备")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request: deviceId={}, message={}", request.deviceId(), request.message());

        ChatResponse response = chatService.chat(
                request.deviceId(),
                request.message(),
                request.sessionId()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/chat/session/{sessionId}")
    @Operation(summary = "清除会话", description = "清除指定会话的对话历史")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        log.info("Clear session: {}", sessionId);
        chatService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
