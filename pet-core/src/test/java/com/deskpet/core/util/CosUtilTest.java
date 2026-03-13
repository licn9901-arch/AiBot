package com.deskpet.core.util;

import com.deskpet.core.config.CosProperties;
import com.deskpet.core.error.BusinessException;
import com.qcloud.cos.COSClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosUtilTest {

    private CosProperties cosProperties;
    private CosUtil cosUtil;

    @BeforeEach
    void setUp() {
        cosProperties = new CosProperties();
        cosProperties.setBucket("deskpet-1250000000");
        cosProperties.setRegion("ap-guangzhou");
        cosProperties.setSecretId("testSecretId");
        cosProperties.setSecretKey("testSecretKey");
        cosProperties.setBasePath("uploads");
        cosProperties.setDefaultSignDuration(Duration.ofMinutes(15));

        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-11T08:00:00Z"), ZoneOffset.UTC);
        cosUtil = new CosUtil(cosProperties, fixedClock);
    }

    @AfterEach
    void tearDown() {
        cosUtil.shutdown();
    }

    @Test
    void isConfigured_whenCoreFieldsPresent_returnsTrue() {
        assertTrue(cosUtil.isConfigured());
    }

    @Test
    void getClient_returnsSdkClient() {
        COSClient client = cosUtil.getClient();

        assertNotNull(client);
    }

    @Test
    void generateObjectKey_includesBasePathBusinessAndDate() {
        String objectKey = cosUtil.generateObjectKey("avatars", "photo.PNG");

        assertTrue(objectKey.startsWith("uploads/avatars/2026/03/11/"));
        assertTrue(objectKey.endsWith(".png"));
    }

    @Test
    void normalizeObjectKey_removesDuplicateSeparators() {
        String normalized = cosUtil.normalizeObjectKey("//uploads\\avatar///demo.png/");

        assertEquals("uploads/avatar/demo.png", normalized);
    }

    @Test
    void buildObjectUrl_returnsCosUrl() {
        String url = cosUtil.buildObjectUrl("uploads/avatar/demo.png");

        assertEquals("https://deskpet-1250000000.cos.ap-guangzhou.myqcloud.com/uploads/avatar/demo.png", url);
    }

    @Test
    void resolveObjectKey_whenInputIsObjectUrl_returnsKey() {
        String objectKey = cosUtil.resolveObjectKey("https://deskpet-1250000000.cos.ap-guangzhou.myqcloud.com/uploads/avatar/demo.png");

        assertEquals("uploads/avatar/demo.png", objectKey);
    }

    @Test
    void resolveObjectUrl_whenInputIsObjectKey_returnsPresignedGetUrl() {
        String url = cosUtil.resolveObjectUrl("uploads/avatar/demo.png");

        assertTrue(url.startsWith("https://deskpet-1250000000.cos.ap-guangzhou.myqcloud.com/uploads/avatar/demo.png?"));
        assertTrue(url.contains("q-signature="));
        assertTrue(url.contains("q-ak=testSecretId"));
    }

    @Test
    void resolveObjectUrl_whenInputIsOwnCosUrl_returnsPresignedGetUrl() {
        String url = cosUtil.resolveObjectUrl("https://deskpet-1250000000.cos.ap-guangzhou.myqcloud.com/uploads/avatar/demo.png");

        assertTrue(url.startsWith("https://deskpet-1250000000.cos.ap-guangzhou.myqcloud.com/uploads/avatar/demo.png?"));
        assertTrue(url.contains("q-signature="));
    }

    @Test
    void resolveObjectUrl_whenCosNotConfigured_returnsOriginalValue() {
        cosProperties.setSecretKey(null);

        String url = cosUtil.resolveObjectUrl("uploads/avatar/demo.png");

        assertEquals("uploads/avatar/demo.png", url);
    }

    @Test
    void generatePresignedGetUrl_returnsSdkSignedUrl() {
        URL url = cosUtil.generatePresignedGetUrl("uploads/avatar/demo.png", Duration.ofMinutes(10));

        assertEquals("https", url.getProtocol());
        assertEquals("deskpet-1250000000.cos.ap-guangzhou.myqcloud.com", url.getHost());
        assertTrue(url.getQuery().contains("q-sign-algorithm=sha1"));
        assertTrue(url.getQuery().contains("q-ak=testSecretId"));
        assertTrue(url.getQuery().contains("q-signature="));
    }

    @Test
    void generatePresignedPutUrl_containsSignature() {
        URL url = cosUtil.generatePresignedPutUrl("uploads/avatar/demo.png", Duration.ofMinutes(5), "image/png");

        assertEquals("https", url.getProtocol());
        assertTrue(url.getQuery().contains("q-signature="));
    }

    @Test
    void buildObjectUrl_whenObjectKeyBlank_throws() {
        assertThrows(BusinessException.class, () -> cosUtil.buildObjectUrl("   "));
    }

    @Test
    void isConfigured_whenMissingSecretKey_returnsFalse() {
        cosProperties.setSecretKey(null);

        assertFalse(cosUtil.isConfigured());
    }
}
