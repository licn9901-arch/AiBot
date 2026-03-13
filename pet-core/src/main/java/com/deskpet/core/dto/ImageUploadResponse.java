package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ImageUploadResponse(
    @Schema(description = "业务类型", example = "avatar")
    String bizType,

    @Schema(description = "对象存储 key", example = "cubee/avatars/2026/03/11/abc123.png")
    String objectKey,

    @Schema(description = "直传地址", example = "https://example.cos.ap-guangzhou.myqcloud.com/cubee/avatars/2026/03/11/abc123.png?q-sign-algorithm=sha1")
    String uploadUrl,

    @Schema(description = "可直接保存的图片访问 URL", example = "https://example.com/cubee/avatars/2026/03/11/abc123.png")
    String url,

    @Schema(description = "上传方法", example = "PUT")
    String method,

    @Schema(description = "直传时需要附带的请求头")
    Map<String, String> headers
) {
}
