package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.deskpet.core.service.GatewayClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/gateway")
@Tag(name = "网关管理", description = "网关状态与指标查询")
public class AdminGatewayController {

    @Autowired
    private GatewayClient gatewayClient;

    @GetMapping("/metrics")
    @SaCheckPermission("device:list")
    @Operation(summary = "获取网关 Metrics", description = "从 MQTT 网关获取运行指标")
    public Map<String, Object> getGatewayMetrics() {
        Map<String, Object> metrics = gatewayClient.fetchMetrics();
        if (metrics == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接到网关");
        }
        return metrics;
    }
}
