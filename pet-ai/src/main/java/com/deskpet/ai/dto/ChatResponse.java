package com.deskpet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "对话响应")
public record ChatResponse(
        @Schema(description = "AI回复", example = "好的，小宠正在往前移动~")
        String reply,
        @Schema(description = "执行的动作列表")
        List<ActionResult> actions,
        @Schema(description = "会话ID", example = "session-uuid")
        String sessionId
) {
    @Schema(description = "动作执行结果")
    public record ActionResult(
            @Schema(description = "指令类型", example = "move")
            String type,
            @Schema(description = "请求ID", example = "uuid")
            String reqId,
            @Schema(description = "执行状态", example = "SENT")
            String status,
            @Schema(description = "错误信息（如有）")
            String error
    ) {
    }
}
