package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.deskpet.core.dto.ImageUploadPrepareRequest;
import com.deskpet.core.dto.ImageUploadResponse;
import com.deskpet.core.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "图片上传")
public class FileUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/images/presign")
    @SaCheckLogin
    @Operation(summary = "获取图片直传地址", description = "返回图片直传 COS 所需的预签名地址和访问 URL")
    public ImageUploadResponse prepareImageUpload(@Valid @RequestBody ImageUploadPrepareRequest request) {
        return imageUploadService.prepareImageUpload(request);
    }
}
