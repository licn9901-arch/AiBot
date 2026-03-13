package com.deskpet.core.controller;

import com.deskpet.core.dto.ImageUploadPrepareRequest;
import com.deskpet.core.dto.ImageUploadResponse;
import com.deskpet.core.error.GlobalExceptionHandler;
import com.deskpet.core.service.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private ImageUploadService imageUploadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FileUploadController(imageUploadService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void prepareImageUpload_returnsUploadResult() throws Exception {
        ImageUploadPrepareRequest request = new ImageUploadPrepareRequest("avatar", "avatar.png", "image/png", 5L);
        when(imageUploadService.prepareImageUpload(request))
            .thenReturn(new ImageUploadResponse(
                "avatar",
                "cubee/avatars/2026/03/11/test.png",
                "https://upload.test/test.png",
                "https://cdn.test/test.png",
                "PUT",
                Map.of("Content-Type", "image/png")
            ));

        mockMvc.perform(post("/api/files/images/presign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "bizType": "avatar",
                      "fileName": "avatar.png",
                      "contentType": "image/png",
                      "size": 5
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bizType").value("avatar"))
            .andExpect(jsonPath("$.url").value("https://cdn.test/test.png"))
            .andExpect(jsonPath("$.uploadUrl").value("https://upload.test/test.png"))
            .andExpect(jsonPath("$.objectKey").value("cubee/avatars/2026/03/11/test.png"))
            .andExpect(jsonPath("$.headers.Content-Type").value("image/png"));

        verify(imageUploadService).prepareImageUpload(request);
    }
}
