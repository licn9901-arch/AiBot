package com.deskpet.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 腾讯云 COS 配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {

    /**
     * 存储桶名称，格式通常为 bucket-appId
     */
    private String bucket;

    /**
     * 地域，例如 ap-guangzhou
     */
    private String region;

    /**
     * SecretId
     */
    private String secretId;

    /**
     * SecretKey
     */
    private String secretKey;

    /**
     * 临时密钥 token，可选
     */
    private String sessionToken;

    /**
     * 自定义访问域名，可选
     */
    private String customDomain;

    /**
     * 对象统一前缀，可选
     */
    private String basePath;

    /**
     * 是否使用 HTTPS
     */
    private boolean useHttps = true;

    /**
     * 默认签名有效期
     */
    private Duration defaultSignDuration = Duration.ofMinutes(15);

    /**
     * 连接超时（毫秒）
     */
    private Integer connectionTimeoutMillis = 30_000;

    /**
     * 读写超时（毫秒）
     */
    private Integer socketTimeoutMillis = 30_000;
}
