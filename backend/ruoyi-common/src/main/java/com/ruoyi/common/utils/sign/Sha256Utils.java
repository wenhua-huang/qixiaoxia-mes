package com.ruoyi.common.utils.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SHA-256 摘要工具
 *
 * 用于外部 API Key 校验：存储与比对均用 hex 摘要，明文不落库不比对。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
public class Sha256Utils
{
    private Sha256Utils() {}

    /**
     * 计算字符串的 SHA-256 hex（小写）
     */
    public static String hash(String s)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest)
            {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException("SHA-256 摘要失败", e);
        }
    }
}
