package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import com.deskpet.core.dto.GenerateBatchResult;
import com.deskpet.core.dto.GenerateLicenseRequest;
import com.deskpet.core.dto.GenerateLicenseResponse;
import com.deskpet.core.dto.LicenseCodeResponse;
import com.deskpet.core.dto.LicenseQueryRequest;
import com.deskpet.core.model.LicenseCode;
import com.deskpet.core.model.PendingDeviceSecret;
import com.deskpet.core.service.LicenseCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 授权码管理控制器（管理员）
 */
@RestController
@RequestMapping("/api/admin/licenses")
@RequiredArgsConstructor
@Tag(name = "授权码管理", description = "授权码生成、查询、撤销、导出")
public class AdminLicenseController {

    private final LicenseCodeService licenseCodeService;

    @PostMapping("/generate")
    @SaCheckPermission("license:generate")
    @Operation(summary = "批量生成授权码")
    public GenerateBatchResult generate(@Valid @RequestBody GenerateLicenseRequest request) {
        return licenseCodeService.generateBatch(request);
    }

    @GetMapping("/batch/{batchNo}/download")
    @SaCheckPermission("license:generate")
    @Operation(summary = "下载批次密钥Excel", description = "下载指定批次的授权码和设备密钥Excel文件")
    public void downloadBatch(@PathVariable String batchNo, HttpServletResponse response) throws IOException {
        List<PendingDeviceSecret> secrets = licenseCodeService.findPendingSecrets(batchNo);
        if (secrets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该批次无待下载的密钥数据（可能已确认下载或已过期清理）");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("licenses-" + batchNo + ".xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        // 构建 Excel 数据
        List<List<String>> data = secrets.stream()
            .map(s -> List.of(s.getCode(), s.getDeviceId(), s.getRawSecret(), s.getProductKey()))
            .toList();

        EasyExcel.write(response.getOutputStream())
            .sheet("授权码密钥")
            .head(List.of(
                List.of("授权码"),
                List.of("设备SN"),
                List.of("设备密钥"),
                List.of("产品标识")
            ))
            .doWrite(data);
    }

    @PostMapping("/batch/{batchNo}/confirm")
    @SaCheckPermission("license:generate")
    @Operation(summary = "确认批次下载", description = "确认已下载密钥，清除暂存的明文密钥")
    public Map<String, Object> confirmBatch(@PathVariable String batchNo) {
        int deleted = licenseCodeService.confirmAndDeletePendingSecrets(batchNo);
        return Map.of("batchNo", batchNo, "deleted", deleted);
    }

    @GetMapping
    @SaCheckPermission("license:list")
    @Operation(summary = "授权码列表", description = "支持按状态、批次号筛选")
    public Page<LicenseCodeResponse> list(
            @RequestParam(name = "status", required = false) LicenseCode.Status status,
            @RequestParam(name = "batchNo", required = false) String batchNo,
            @PageableDefault(size = 20) Pageable pageable) {
        return licenseCodeService.list(new LicenseQueryRequest(status, batchNo), pageable);
    }

    @PutMapping("/{id}/revoke")
    @SaCheckPermission("license:revoke")
    @Operation(summary = "撤销授权码", description = "撤销后设备解绑")
    public void revoke(@PathVariable Long id) {
        licenseCodeService.revoke(id);
    }

    @GetMapping("/export")
    @SaCheckPermission("license:export")
    @Operation(summary = "导出授权码", description = "导出为 CSV 格式")
    public void export(
            @RequestParam(name = "status", required = false) LicenseCode.Status status,
            @RequestParam(name = "batchNo", required = false) String batchNo,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=licenses.csv");

        PrintWriter writer = response.getWriter();
        // 添加 BOM 以支持 Excel 打开中文
        writer.write('\ufeff');
        licenseCodeService.exportCsv(writer, new LicenseQueryRequest(status, batchNo));
        writer.flush();
    }

    @GetMapping("/batch/{batchNo}/stats")
    @SaCheckPermission("license:list")
    @Operation(summary = "批次统计", description = "统计指定批次的授权码使用情况")
    public Map<String, Object> getBatchStats(@PathVariable String batchNo) {
        return licenseCodeService.getBatchStats(batchNo);
    }
}
