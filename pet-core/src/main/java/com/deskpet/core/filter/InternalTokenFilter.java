package com.deskpet.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import com.deskpet.core.error.ErrorCode;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {
    private final String internalToken;

    public InternalTokenFilter(@Value("${internal.token:}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (internalToken == null || internalToken.isBlank()) {
            return true;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.startsWith("/internal");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("X-Internal-Token");
        if (!internalToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String body = "{\"code\":\"" + ErrorCode.INTERNAL_TOKEN_INVALID.code()
                    + "\",\"message\":\"" + ErrorCode.INTERNAL_TOKEN_INVALID.defaultMessage() + "\"}";
            response.getWriter().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
