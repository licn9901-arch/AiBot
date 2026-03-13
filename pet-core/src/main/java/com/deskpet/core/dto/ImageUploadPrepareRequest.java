package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageUploadPrepareRequest(
    @Schema(description = "业务类型", example = "avatar")
    @NotBlank(message = "bizType 不能为空")
    String bizType,

    @Schema(description = "原始文件名", example = "avatar.png")
    @NotBlank(message = "fileName 不能为空")
    @Size(max = 255, message = "fileName 长度不能超过255")
    String fileName,

    @Schema(description = "文件内容类型", example = "image/png")
    @NotBlank(message = "contentType 不能为空")
    @Size(max = 100, message = "contentType 长度不能超过100")
    String contentType,

    @Schema(description = "文件大小，单位字节", example = "102400")
    @Min(value = 1, message = "size 必须大于 0")
    long size
) {
}
