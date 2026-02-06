package com.deskpet.core.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * 密码工具类（使用 BCrypt 加密）
 */
public final class PasswordUtil {

    private static final int COST = 10;

    private PasswordUtil() {
    }

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword);
        return result.verified;
    }

    /**
     * 生成密码 hash（用于初始化数据）
     * 可在本地运行此方法生成有效的 BCrypt hash
     */
    public static void main(String[] args) {
        String password = "admin123";
        String hash = encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("Verify: " + matches(password, hash));
    }
}
