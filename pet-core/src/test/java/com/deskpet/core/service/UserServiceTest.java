package com.deskpet.core.service;

import com.deskpet.core.dto.ForgotPasswordRequest;
import com.deskpet.core.dto.UserLoginRequest;
import com.deskpet.core.dto.UserRegisterRequest;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.AuthToken;
import com.deskpet.core.model.AuthTokenType;
import com.deskpet.core.model.SysRole;
import com.deskpet.core.model.SysUser;
import com.deskpet.core.repository.SysRoleRepository;
import com.deskpet.core.repository.SysUserRepository;
import com.deskpet.core.util.CosUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserRepository userRepository;
    @Mock
    private SysRoleRepository roleRepository;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private AuthMailService authMailService;
    @Mock
    private CosUtil cosUtil;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository,
            roleRepository,
            operationLogService,
            permissionService,
            authTokenService,
            authMailService,
            cosUtil
        );
    }

    @Test
    void register_createsPendingUserAndSendsActivationMail() {
        SysRole userRole = SysRole.builder().id(1L).code("USER").name("用户").build();
        when(userRepository.existsByUsername("tester")).thenReturn(false);
        when(userRepository.existsByEmail("tester@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("13800000000")).thenReturn(false);
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            return user;
        });
        when(authTokenService.createActivationToken(any(SysUser.class))).thenReturn("activation-token");
        when(authTokenService.buildActivationLink("activation-token")).thenReturn("https://deskpet.test/activate?token=activation-token");

        userService.register(new UserRegisterRequest(
            "tester",
            "password123",
            "tester@example.com",
            "13800000000"
        ));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userRepository).save(captor.capture());
        SysUser savedUser = captor.getValue();
        assertEquals("tester", savedUser.getUsername());
        assertEquals("PENDING_ACTIVATION", savedUser.getStatus());
        assertEquals("tester@example.com", savedUser.getEmail());
        assertNotEquals("password123", savedUser.getPasswordHash());
        assertEquals(1, savedUser.getRoles().size());
        verify(authMailService).sendActivationEmail(
            eq("tester@example.com"),
            eq("tester"),
            eq("https://deskpet.test/activate?token=activation-token")
        );
    }

    @Test
    void activate_marksUserActive() {
        SysUser user = SysUser.builder()
            .id(101L)
            .username("tester")
            .email("tester@example.com")
            .passwordHash("hash")
            .status("PENDING_ACTIVATION")
            .build();
        AuthToken authToken = AuthToken.builder()
            .id(1L)
            .user(user)
            .tokenHash("hash")
            .tokenType(AuthTokenType.ACTIVATION)
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
        when(authTokenService.consumeValidToken("token-1", AuthTokenType.ACTIVATION)).thenReturn(authToken);

        userService.activate("token-1");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userRepository).save(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void forgotPassword_activeUser_sendsResetMail() {
        SysUser user = SysUser.builder()
            .id(102L)
            .username("tester")
            .email("tester@example.com")
            .passwordHash("hash")
            .status("ACTIVE")
            .build();
        when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(user));
        when(authTokenService.createPasswordResetToken(user)).thenReturn("reset-token");
        when(authTokenService.buildPasswordResetLink("reset-token")).thenReturn("https://deskpet.test/reset-password?token=reset-token");

        userService.forgotPassword(new ForgotPasswordRequest("tester@example.com"));

        verify(authMailService).sendPasswordResetEmail(
            eq("tester@example.com"),
            eq("tester"),
            eq("https://deskpet.test/reset-password?token=reset-token")
        );
    }

    @Test
    void forgotPassword_unknownUser_isSilent() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        userService.forgotPassword(new ForgotPasswordRequest("ghost@example.com"));

        verify(authTokenService, never()).createPasswordResetToken(any());
        verify(authMailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void login_pendingActivation_throwsNotActivated() {
        SysUser user = SysUser.builder()
            .id(103L)
            .username("tester")
            .email("tester@example.com")
            .passwordHash(com.deskpet.core.util.PasswordUtil.encode("password123"))
            .status("PENDING_ACTIVATION")
            .build();
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.login(new UserLoginRequest("tester", "password123"), "127.0.0.1", "JUnit"));

        assertEquals(ErrorCode.USER_NOT_ACTIVATED, exception.getErrorCode());
    }
}
