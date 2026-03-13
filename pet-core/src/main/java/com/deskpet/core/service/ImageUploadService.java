package com.deskpet.core.service;

import com.deskpet.core.dto.ImageUploadResponse;
import com.deskpet.core.dto.ImageUploadPrepareRequest;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.util.CosUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> BIZ_TYPE_DIRECTORY = Map.of(
        "avatar", "avatars",
        "product-icon", "product-icons"
    );

    private final CosUtil cosUtil;

    public ImageUploadResponse prepareImageUpload(ImageUploadPrepareRequest request) {
        validateBizType(request.bizType());
        validateFile(request.fileName(), request.contentType(), request.size());

        if (!cosUtil.isConfigured()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "图片上传服务未配置");
        }

        CosUtil.CosObjectLocation location = cosUtil.createObjectLocation(
            BIZ_TYPE_DIRECTORY.get(request.bizType()),
            request.fileName()
        );
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", request.contentType());

        return new ImageUploadResponse(
            request.bizType(),
            location.objectKey(),
            cosUtil.generatePresignedPutUrl(location.objectKey(), request.contentType()).toString(),
            location.objectUrl(),
            "PUT",
            headers
        );
    }

    private void validateBizType(String bizType) {
        if (!BIZ_TYPE_DIRECTORY.containsKey(bizType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bizType 不支持");
        }
    }

    private void validateFile(String fileName, String contentType, long size) {
        if (size <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "上传文件不能为空");
        }
        if (size > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "图片大小不能超过 5MB");
        }

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "仅支持 JPG、PNG、WEBP、GIF 图片");
        }

        String extension = extractExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "图片扩展名不合法");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
