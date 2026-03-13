package com.deskpet.core.service;

import com.deskpet.core.config.AuthProperties;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.AuthToken;
import com.deskpet.core.model.AuthTokenType;
import com.deskpet.core.model.SysUser;
import com.deskpet.core.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthTokenRepository authTokenRepository;
    private final AuthProperties authProperties;

    @Transactional(rollbackFor = Exception.class)
    public String createActivationToken(SysUser user) {
        return createToken(user, AuthTokenType.ACTIVATION, authProperties.getActivationTokenTtl().getSeconds());
    }

    @Transactional(rollbackFor = Exception.class)
    public String createPasswordResetToken(SysUser user) {
        return createToken(user, AuthTokenType.PASSWORD_RESET, authProperties.getPasswordResetTokenTtl().getSeconds());
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthToken consumeValidToken(String rawToken, AuthTokenType expectedType) {
        AuthToken token = getValidToken(rawToken, expectedType);
        token.setUsedAt(Instant.now());
        return authTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public AuthToken getValidToken(String rawToken, AuthTokenType expectedType) {
        AuthToken token = authTokenRepository.findByTokenHash(hashToken(rawToken))
            .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID_OR_USED, "token 无效或已使用"));

        if (token.getTokenType() != expectedType) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID_OR_USED, "token 无效或已使用");
        }
        if (token.isUsed()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID_OR_USED, "token 无效或已使用");
        }
        if (token.isExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "token 已过期");
        }
        return token;
    }

    @Transactional(rollbackFor = Exception.class)
    public void invalidateUnusedTokens(Long userId, AuthTokenType tokenType) {
        List<AuthToken> tokens = authTokenRepository.findAllByUser_IdAndTokenTypeAndUsedAtIsNull(userId, tokenType);
        if (tokens.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        tokens.forEach(token -> token.setUsedAt(now));
        authTokenRepository.saveAll(tokens);
    }

    public String buildActivationLink(String rawToken) {
        return authProperties.getFrontendBaseUrl() + "/auth/activate?token=" + rawToken;
    }

    public String buildPasswordResetLink(String rawToken) {
        return authProperties.getFrontendBaseUrl() + "/auth/reset-password?token=" + rawToken;
    }

    private String createToken(SysUser user, AuthTokenType tokenType, long ttlSeconds) {
        invalidateUnusedTokens(user.getId(), tokenType);

        String rawToken = generateToken();
        AuthToken authToken = AuthToken.builder()
            .user(user)
            .tokenHash(hashToken(rawToken))
            .tokenType(tokenType)
            .expiresAt(Instant.now().plusSeconds(ttlSeconds))
            .build();
        authTokenRepository.save(authToken);
        return rawToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "token hash 生成失败");
        }
    }
}
