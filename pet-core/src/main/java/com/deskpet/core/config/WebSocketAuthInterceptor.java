package com.deskpet.core.config;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket STOMP CONNECT 阶段认证拦截器
 * 从 STOMP header 中提取 Sa-Token 并验证登录状态
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 用 Sa-Token 验证
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                throw new MessagingException("未登录");
            }
            // 将 loginId 存入 session attributes，供后续使用
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("loginId", loginId);
            }
        }
        return message;
    }
}
