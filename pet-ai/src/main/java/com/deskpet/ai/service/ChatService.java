package com.deskpet.ai.service;

import com.deskpet.ai.dto.ChatResponse;
import com.deskpet.ai.tool.DeviceToolsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话服务 - 处理用户对话并调用 AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;

    @Value("${chat.max-history:10}")
    private int maxHistory;

    // 简单的会话存储（生产环境应使用 Redis）
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();

    /**
     * 处理对话请求
     */
    public ChatResponse chat(String deviceId, String userMessage, String sessionId) {
        // 生成或使用现有会话ID
        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();

        // 设置当前设备ID（供 Tool 使用）
        DeviceToolsConfig.setCurrentDeviceId(deviceId);

        // 获取或创建会话历史
        List<Message> history = sessionHistory.computeIfAbsent(sid, k -> new ArrayList<>());

        // 添加用户消息到历史
        history.add(new UserMessage(userMessage));

        // 保持历史长度
        trimHistory(history);

        try {
            // 构建 ChatClient
            ChatClient chatClient = chatClientBuilder
                    .defaultSystem(getSystemPrompt(deviceId))
                    .defaultFunctions("sendMoveCommand", "sendStopCommand", "setEmotion", "getDeviceState")
                    .build();

            // 发送请求
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            // 添加助手回复到历史
            history.add(new AssistantMessage(response));

            log.info("Chat response for device {}: {}", deviceId, response);

            // 构建响应
            return new ChatResponse(response, List.of(), sid);

        } catch (Exception e) {
            log.error("Chat error for device {}: {}", deviceId, e.getMessage(), e);
            return new ChatResponse("抱歉，我遇到了一些问题：" + e.getMessage(), List.of(), sid);
        } finally {
            // 清理 ThreadLocal
            DeviceToolsConfig.clearCurrentDeviceId();
        }
    }

    /**
     * 获取系统提示词
     */
    private String getSystemPrompt(String deviceId) {
        return """
                你是一个可爱的桌宠助手，负责控制一个名叫"小宠"的桌面机器人（DeskPet）。
                当前控制的设备ID是：%s

                ## 你的能力
                - 控制小宠移动（前进、后退、左转、右转）
                - 控制小宠表情（开心、难过、生气、困倦、空闲）
                - 查询小宠状态（电量、信号、是否在线）

                ## 指令映射
                - 用户说"往前走"、"前进"、"向前"等 → 调用 sendMoveCommand(direction="forward")
                - 用户说"后退"、"往后"等 → 调用 sendMoveCommand(direction="backward")
                - 用户说"左转"、"往左"等 → 调用 sendMoveCommand(direction="left")
                - 用户说"右转"、"往右"等 → 调用 sendMoveCommand(direction="right")
                - 用户说"停"、"停下"、"别动"等 → 调用 sendStopCommand()
                - 用户说"开心点"、"笑一个"、"高兴"等 → 调用 setEmotion(emotion="happy")
                - 用户说"难过"、"伤心"等 → 调用 setEmotion(emotion="sad")
                - 用户说"生气"、"发怒"等 → 调用 setEmotion(emotion="angry")
                - 用户说"困了"、"睡觉"等 → 调用 setEmotion(emotion="sleepy")
                - 用户说"状态"、"电量"、"怎么样"等 → 调用 getDeviceState()

                ## 行为准则
                1. 用简短可爱的语气回复，符合桌宠的人设
                2. 执行指令后要告诉用户结果
                3. 不确定用户意图时，友好地询问
                4. 如果设备离线，要提醒用户

                ## 安全限制
                - 速度默认 0.5，最高不超过 0.8
                - 移动时间默认 1000ms，最长不超过 3000ms
                - 遇到危险指令（如"全速冲"）要拒绝
                """.formatted(deviceId);
    }

    /**
     * 修剪历史记录
     */
    private void trimHistory(List<Message> history) {
        while (history.size() > maxHistory * 2) {
            history.remove(0);
        }
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        sessionHistory.remove(sessionId);
    }
}
