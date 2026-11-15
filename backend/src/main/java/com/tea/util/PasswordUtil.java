package com.tea.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码摘要工具：数据库只保存 SHA-256 摘要，不保存明文。
 * 与 init.sql 中的 SHA2('xxx', 256) 结果一致（64 位小写十六进制）。
 * <p>
 * 说明：生产环境应使用 BCrypt 等加盐慢哈希，本项目只依赖 JDK，先用 SHA-256 体现"不存明文"的思路。
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
