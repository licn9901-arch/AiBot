package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.CommandCreateRequest;
import com.deskpet.core.dto.CommandResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Command;
import com.deskpet.core.service.CommandService;
import com.deskpet.core.service.LicenseCodeService;
import com.deskpet.core.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/devices/{deviceId}/commands")
@RequiredArgsConstructor
@Tag(name = "指令管理", description = "下发指令与结果查询")
public class CommandController {

    private final CommandService commandService;
    private final LicenseCodeService licenseCodeService;
    private final OperationLogService operationLogService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SaCheckPermission("device:control")
    @Operation(summary = "创建指令", description = "下发指令到指定设备")
    public CommandResponse createCommand(@PathVariable String deviceId,
                                         @Valid @RequestBody CommandCreateRequest request) {
        // 非管理员需校验设备归属
        long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasRole("ADMIN") && !licenseCodeService.hasDevice(userId, deviceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此设备");
        }

        Command command = commandService.createCommand(deviceId, request.type(), request.payload());

        // 记录操作日志
        operationLogService.log(userId, deviceId, "SEND_COMMAND",
            Map.of("type", request.type(), "reqId", command.reqId()));

        return CommandResponse.of(command);
    }

    @PostMapping("/{reqId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SaCheckPermission("device:control")
    @Operation(summary = "重试指令", description = "仅 TIMEOUT/FAILED 状态允许重试")
    public CommandResponse retryCommand(@PathVariable String deviceId,
                                        @PathVariable String reqId) {
        // 非管理员需校验设备归属
        long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasRole("ADMIN") && !licenseCodeService.hasDevice(userId, deviceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此设备");
        }

        Command command = commandService.retryCommand(deviceId, reqId);

        // 记录操作日志
        operationLogService.log(userId, deviceId, "RETRY_COMMAND",
            Map.of("reqId", reqId));

        return CommandResponse.of(command);
    }

    @GetMapping("/{reqId}")
    @SaCheckPermission("device:view")
    @Operation(summary = "查询指令", description = "查询指令状态与回执信息")
    public CommandResponse getCommand(@PathVariable String deviceId,
                                      @PathVariable String reqId) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }

        Command command = commandService.findCommand(reqId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMAND_NOT_FOUND));
        return CommandResponse.of(command);
    }
}
