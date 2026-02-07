package com.deskpet.core.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 登录校验 -- 排除不需要登录的接口
            SaRouter.match("/**")
                // 排除公开接口
                .notMatch(
                    "/api/auth/login",
                    "/api/auth/register",
                    // 内部接口（网关调用）
                    "/internal/**",
                    // OpenAPI 文档
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    // Actuator
                    "/actuator/**",
                    // WebSocket 端点（握手走自己的认证拦截器）
                    "/ws/**",
                    // 错误页面
                    "/error"
                )
                .check(r -> StpUtil.checkLogin());

        })).addPathPatterns("/**");
    }
}
