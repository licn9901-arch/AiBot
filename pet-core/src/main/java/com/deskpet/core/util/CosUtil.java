package com.deskpet.core.util;

import com.deskpet.core.config.CosProperties;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 腾讯云 COS 工具类
 * 基于 COS Java SDK 封装常用能力：客户端获取、对象路径生成、访问地址、预签名 URL、上传与删除。
 */
@Component
public class CosUtil {

    private final CosProperties cosProperties;
    private final Clock clock;

    private volatile COSClient cosClient;

    @Autowired
    public CosUtil(CosProperties cosProperties) {
        this(cosProperties, Clock.systemUTC());
    }

    CosUtil(CosProperties cosProperties, Clock clock) {
        this.cosProperties = cosProperties;
        this.clock = clock;
    }

    /**
     * COS 配置是否完整。
     */
    public boolean isConfigured() {
        return hasText(cosProperties.getBucket())
            && hasText(cosProperties.getRegion())
            && hasText(cosProperties.getSecretId())
            && hasText(cosProperties.getSecretKey());
    }

    /**
     * 获取 SDK 客户端，按需懒加载，整个应用复用单例。
     */
    public COSClient getClient() {
        ensureConfigured();
        if (cosClient == null) {
            synchronized (this) {
                if (cosClient == null) {
                    cosClient = createClient();
                }
            }
        }
        return cosClient;
    }

    /**
     * 生成对象 key，格式：basePath/business/yyyy/MM/dd/uuid.ext
     */
    public String generateObjectKey(String businessType, String originalFilename) {
        String normalizedBusinessType = sanitizePathSegment(businessType);
        String extension = extractExtension(originalFilename);
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));

        List<String> segments = new java.util.ArrayList<>();
        if (hasText(cosProperties.getBasePath())) {
            segments.add(normalizeObjectKey(cosProperties.getBasePath()));
        }
        segments.add(normalizedBusinessType);
        segments.add(String.valueOf(today.getYear()));
        segments.add(String.format("%02d", today.getMonthValue()));
        segments.add(String.format("%02d", today.getDayOfMonth()));

        String fileName = UUID.randomUUID().toString().replace("-", "");
        if (hasText(extension)) {
            fileName += "." + extension;
        }
        segments.add(fileName);
        return String.join("/", segments);
    }

    /**
     * 规范化对象 key。
     */
    public String normalizeObjectKey(String objectKey) {
        if (!hasText(objectKey)) {
            return "";
        }
        String normalized = objectKey.trim().replace('\\', '/');
        normalized = normalized.replaceAll("/+", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 构建对象访问 URL。
     */
    public String buildObjectUrl(String objectKey) {
        String normalizedObjectKey = requireObjectKey(objectKey);
        return buildBaseUrl() + "/" + encodePath(normalizedObjectKey);
    }

    public String resolveObjectKey(String objectKeyOrUrl) {
        if (!hasText(objectKeyOrUrl)) {
            return "";
        }
        String raw = objectKeyOrUrl.trim();
        URI uri = parseUri(raw);
        if (uri == null || !hasText(uri.getHost())) {
            return normalizeObjectKey(raw);
        }
        if (!isOwnObjectHost(uri.getHost())) {
            return raw;
        }
        return normalizeObjectKey(URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8));
    }

    public String resolveObjectUrl(String objectKeyOrUrl) {
        if (!hasText(objectKeyOrUrl)) {
            return null;
        }
        String normalizedObjectKey = resolveObjectKey(objectKeyOrUrl);
        if (!hasText(normalizedObjectKey)) {
            return null;
        }
        URI uri = parseUri(objectKeyOrUrl.trim());
        if (uri != null && hasText(uri.getHost()) && !isOwnObjectHost(uri.getHost())) {
            return objectKeyOrUrl.trim();
        }
        if (!isConfigured()) {
            return objectKeyOrUrl.trim();
        }
        return generatePresignedGetUrl(normalizedObjectKey).toString();
    }

    /**
     * 生成 GET 预签名 URL。
     */
    public URL generatePresignedGetUrl(String objectKey) {
        return generatePresignedGetUrl(objectKey, cosProperties.getDefaultSignDuration());
    }

    /**
     * 生成 GET 预签名 URL。
     */
    public URL generatePresignedGetUrl(String objectKey, Duration duration) {
        return generatePresignedUrl(objectKey, HttpMethodName.GET, duration, Map.of(), Map.of());
    }

    /**
     * 生成 PUT 预签名 URL，可用于客户端直传。
     */
    public URL generatePresignedPutUrl(String objectKey, Duration duration, String contentType) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (hasText(contentType)) {
            headers.put("Content-Type", contentType);
        }
        return generatePresignedUrl(objectKey, HttpMethodName.PUT, duration, headers, Map.of());
    }

    /**
     * 使用默认有效期生成 PUT 预签名 URL。
     */
    public URL generatePresignedPutUrl(String objectKey, String contentType) {
        return generatePresignedPutUrl(objectKey, cosProperties.getDefaultSignDuration(), contentType);
    }

    /**
     * 生成任意方法的预签名 URL。
     */
    public URL generatePresignedUrl(String objectKey,
                                    HttpMethodName method,
                                    Duration duration,
                                    Map<String, String> headers,
                                    Map<String, String> params) {
        String normalizedObjectKey = requireObjectKey(objectKey);
        Date expiration = new Date(Instant.now(clock).plusSeconds(normalizeSignSeconds(duration)).toEpochMilli());
        return getClient().generatePresignedUrl(
            cosProperties.getBucket(),
            normalizedObjectKey,
            expiration,
            method,
            headers == null ? Map.of() : headers,
            params == null ? Map.of() : params
        );
    }

    /**
     * 上传本地文件。
     */
    public PutObjectResult uploadFile(String objectKey, File file) {
        String normalizedObjectKey = requireObjectKey(objectKey);
        if (file == null || !file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "上传文件不存在");
        }
        return getClient().putObject(new PutObjectRequest(cosProperties.getBucket(), normalizedObjectKey, file));
    }

    /**
     * 上传输入流。
     */
    public PutObjectResult uploadStream(String objectKey,
                                        InputStream inputStream,
                                        long contentLength,
                                        String contentType) {
        String normalizedObjectKey = requireObjectKey(objectKey);
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "上传流不能为空");
        }
        ObjectMetadata metadata = new ObjectMetadata();
        if (contentLength >= 0) {
            metadata.setContentLength(contentLength);
        }
        if (hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        PutObjectRequest request = new PutObjectRequest(cosProperties.getBucket(), normalizedObjectKey, inputStream, metadata);
        return getClient().putObject(request);
    }

    /**
     * 对象是否存在。
     */
    public boolean doesObjectExist(String objectKey) {
        return getClient().doesObjectExist(cosProperties.getBucket(), requireObjectKey(objectKey));
    }

    /**
     * 删除对象。
     */
    public void deleteObject(String objectKey) {
        getClient().deleteObject(cosProperties.getBucket(), requireObjectKey(objectKey));
    }

    /**
     * 生成对象位置描述。
     */
    public CosObjectLocation createObjectLocation(String businessType, String originalFilename) {
        String objectKey = generateObjectKey(businessType, originalFilename);
        return new CosObjectLocation(objectKey, buildObjectUrl(objectKey));
    }

    @PreDestroy
    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    public record CosObjectLocation(String objectKey, String objectUrl) {
    }

    private COSClient createClient() {
        COSCredentials credentials = hasText(cosProperties.getSessionToken())
            ? new BasicSessionCredentials(
                cosProperties.getSecretId(),
                cosProperties.getSecretKey(),
                cosProperties.getSessionToken()
            )
            : new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());

        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        clientConfig.setHttpProtocol(cosProperties.isUseHttps() ? HttpProtocol.https : HttpProtocol.http);
        if (cosProperties.getConnectionTimeoutMillis() != null) {
            clientConfig.setConnectionTimeout(cosProperties.getConnectionTimeoutMillis());
        }
        if (cosProperties.getSocketTimeoutMillis() != null) {
            clientConfig.setSocketTimeout(cosProperties.getSocketTimeoutMillis());
        }
        clientConfig.setSignExpired((int) cosProperties.getDefaultSignDuration().getSeconds());
        return new COSClient(credentials, clientConfig);
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "COS 配置不完整");
        }
    }

    private String requireObjectKey(String objectKey) {
        String normalized = normalizeObjectKey(objectKey);
        if (!hasText(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "objectKey 不能为空");
        }
        return normalized;
    }

    private String buildBaseUrl() {
        String schema = cosProperties.isUseHttps() ? "https://" : "http://";
        if (hasText(cosProperties.getCustomDomain())) {
            String domain = cosProperties.getCustomDomain().trim();
            if (domain.startsWith("http://") || domain.startsWith("https://")) {
                return domain.replaceAll("/+$", "");
            }
            return schema + domain.replaceAll("/+$", "");
        }
        return schema + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com";
    }

    private boolean canBuildObjectUrl() {
        return hasText(cosProperties.getCustomDomain())
            || (hasText(cosProperties.getBucket()) && hasText(cosProperties.getRegion()));
    }

    private boolean isOwnObjectHost(String host) {
        if (!hasText(host)) {
            return false;
        }
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        if (hasText(cosProperties.getCustomDomain())) {
            URI customDomainUri = parseUri(normalizeCustomDomainForParsing(cosProperties.getCustomDomain()));
            if (customDomainUri != null && hasText(customDomainUri.getHost())
                && normalizedHost.equals(customDomainUri.getHost().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return hasText(cosProperties.getBucket())
            && hasText(cosProperties.getRegion())
            && normalizedHost.equals((cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com")
            .toLowerCase(Locale.ROOT));
    }

    private URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeCustomDomainForParsing(String customDomain) {
        String trimmed = customDomain.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return (cosProperties.isUseHttps() ? "https://" : "http://") + trimmed;
    }

    private long normalizeSignSeconds(Duration duration) {
        Duration actualDuration = Objects.requireNonNullElse(duration, cosProperties.getDefaultSignDuration());
        long seconds = actualDuration.getSeconds();
        if (seconds <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "签名有效期必须大于 0 秒");
        }
        return seconds;
    }

    private String sanitizePathSegment(String raw) {
        if (!hasText(raw)) {
            return "default";
        }
        return raw.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9/_-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("/+", "/")
            .replaceAll("^/+|/+$", "");
    }

    private String extractExtension(String filename) {
        if (!hasText(filename)) {
            return "";
        }
        String cleanFilename = filename.trim();
        int dotIndex = cleanFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanFilename.length() - 1) {
            return "";
        }
        return cleanFilename.substring(dotIndex + 1)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private String encodePath(String path) {
        return List.of(path.split("/"))
            .stream()
            .filter(StringUtils::hasText)
            .map(this::percentEncode)
            .collect(Collectors.joining("/"));
    }

    private String percentEncode(String value) {
        return URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
