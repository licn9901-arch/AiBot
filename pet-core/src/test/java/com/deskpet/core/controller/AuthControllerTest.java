package com.deskpet.core.controller;

import com.deskpet.core.error.GlobalExceptionHandler;
import com.deskpet.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void register_returnsNoContent() throws Exception {
        String body = """
            {
              "username": "tester",
              "password": "password123",
              "email": "tester@example.com",
              "phone": "13800000000"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNoContent());

        verify(userService).register(any());
    }

    @Test
    void forgotPassword_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"tester@example.com"}
                    """))
            .andExpect(status().isNoContent());

        verify(userService).forgotPassword(any());
    }

    @Test
    void activate_returnsNoContent() throws Exception {
        mockMvc.perform(get("/api/auth/activate").param("token", "token-123"))
            .andExpect(status().isNoContent());

        verify(userService).activate("token-123");
    }

    @Test
    void validateResetToken_returnsValidTrue() throws Exception {
        mockMvc.perform(get("/api/auth/reset-password/validate").param("token", "token-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));

        verify(userService).validatePasswordResetToken(eq("token-123"));
    }
}
