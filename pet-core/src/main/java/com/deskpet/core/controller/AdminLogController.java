package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.deskpet.core.model.OperationLog;
import com.deskpet.core.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * 操作日志控制器（管理员）
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志查询")
public class AdminLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @SaCheckPermission("log:list")
    @Operation(summary = "操作日志列表", description = "支持按用户、设备、操作类型、时间范围筛选")
    public Page<OperationLog> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @PageableDefault(size = 20) Pageable pageable) {
        return operationLogService.findByFilters(userId, deviceId, action, startTime, endTime, pageable);
    }
}
