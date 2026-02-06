package com.deskpet.core.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 授权码生成器
 * 格式：DKPT-XXXX-XXXX-XXXX（16位，含分隔符共19位）
 */
public final class LicenseCodeGenerator {

    // 排除易混淆字符：0/O/1/I/L
    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "DKPT";

    private LicenseCodeGenerator() {
    }

    /**
     * 生成单个授权码
     * @return 授权码，格式：DKPT-XXXX-XXXX-XXXX
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX).append("-");
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 批量生成授权码
     * @param count 生成数量
     * @return 授权码列表（保证唯一）
     */
    public static List<String> generateBatch(int count) {
        Set<String> codes = new HashSet<>();
        int maxAttempts = count * 10; // 防止无限循环
        int attempts = 0;
        while (codes.size() < count && attempts < maxAttempts) {
            codes.add(generate());
            attempts++;
        }
        return new ArrayList<>(codes);
    }

    /**
     * 验证授权码格式
     * @param code 授权码
     * @return 是否有效
     */
    public static boolean isValidFormat(String code) {
        if (code == null || code.length() != 19) {
            return false;
        }
        if (!code.startsWith(PREFIX + "-")) {
            return false;
        }
        // 检查格式：DKPT-XXXX-XXXX-XXXX
        String[] parts = code.split("-");
        if (parts.length != 4) {
            return false;
        }
        if (!parts[0].equals(PREFIX)) {
            return false;
        }
        for (int i = 1; i < 4; i++) {
            if (parts[i].length() != 4) {
                return false;
            }
            for (char c : parts[i].toCharArray()) {
                if (CHARS.indexOf(c) < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
