package com.deskpet.core.service;

import com.deskpet.core.dto.ImageUploadPrepareRequest;
import com.deskpet.core.dto.ImageUploadResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.util.CosUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private CosUtil cosUtil;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(cosUtil);
    }

    @Test
    void prepareImageUpload_success() throws Exception {
        ImageUploadPrepareRequest request = new ImageUploadPrepareRequest("avatar", "avatar.png", "image/png", 5L);
        when(cosUtil.isConfigured()).thenReturn(true);
        when(cosUtil.createObjectLocation("avatars", "avatar.png"))
            .thenReturn(new CosUtil.CosObjectLocation("cubee/avatars/2026/03/11/test.png", "https://cdn.test/test.png"));
        when(cosUtil.generatePresignedPutUrl("cubee/avatars/2026/03/11/test.png", "image/png"))
            .thenReturn(java.net.URI.create("https://upload.test/test.png").toURL());

        ImageUploadResponse response = imageUploadService.prepareImageUpload(request);

        assertEquals("https://cdn.test/test.png", response.url());
        assertEquals("cubee/avatars/2026/03/11/test.png", response.objectKey());
        assertEquals("https://upload.test/test.png", response.uploadUrl());
        assertEquals("PUT", response.method());
        assertEquals("image/png", response.headers().get("Content-Type"));
        verify(cosUtil).generatePresignedPutUrl(eq("cubee/avatars/2026/03/11/test.png"), eq("image/png"));
    }

    @Test
    void prepareImageUpload_emptyFile_throws() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> imageUploadService.prepareImageUpload(new ImageUploadPrepareRequest("avatar", "avatar.png", "image/png", 0)));

        assertEquals(ErrorCode.INVALID_PARAM, exception.getErrorCode());
    }

    @Test
    void prepareImageUpload_invalidContentType_throws() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> imageUploadService.prepareImageUpload(new ImageUploadPrepareRequest("avatar", "avatar.txt", "text/plain", 5)));

        assertEquals(ErrorCode.INVALID_PARAM, exception.getErrorCode());
    }

    @Test
    void prepareImageUpload_tooLarge_throws() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> imageUploadService.prepareImageUpload(
                new ImageUploadPrepareRequest("avatar", "avatar.jpg", "image/jpeg", 5 * 1024 * 1024 + 1L)));

        assertEquals(ErrorCode.INVALID_PARAM, exception.getErrorCode());
    }

    @Test
    void prepareImageUpload_cosNotConfigured_throws() {
        when(cosUtil.isConfigured()).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> imageUploadService.prepareImageUpload(
                new ImageUploadPrepareRequest("avatar", "avatar.webp", "image/webp", 5)));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(cosUtil, never()).generatePresignedPutUrl(anyString(), anyString());
    }

    @Test
    void prepareImageUpload_unknownBizType_throws() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> imageUploadService.prepareImageUpload(
                new ImageUploadPrepareRequest("unknown", "avatar.webp", "image/webp", 5)));

        assertEquals(ErrorCode.INVALID_PARAM, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("bizType"));
    }
}
