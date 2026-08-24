package com.ruoyi.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AES-GCM 加解密工具（SMTP 授权码等敏感配置落库前加密）。
 * <p>
 * - 密钥派生：环境变量 MAIL_SECRET_KEY → SHA-256 → 32 字节（AES-256）；
 * - 未配置密钥时降级：encrypt 原样返回（明文入库），页面会提示风险——部署方应设置密钥；
 * - 存储格式："enc:" + Base64(iv + ciphertext)，iv 每次随机（GCM 天然带认证标签）；
 * - decrypt 对无 "enc:" 前缀的旧数据原样返回（明文兼容，幂等迁移）；
 * - 解密失败原样返回并打日志（不抛异常，避免邮件功能因密钥轮换而瘫痪）。
 */
public class AesUtil
{
    private static final Logger log = LoggerFactory.getLogger(AesUtil.class);

    private static final String PREFIX = "enc:";
    private static final String KEY_ENV = "MAIL_SECRET_KEY";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;

    private AesUtil() { }

    /** 密钥：环境变量 SHA-256 派生；未设置返回 null（降级明文） */
    private static byte[] secretKey()
    {
        String env = System.getenv(KEY_ENV);
        if (env == null || env.trim().isEmpty())
        {
            return null;
        }
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(env.trim().getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            log.warn("AES 密钥派生失败，降级明文存储：{}", e.getMessage());
            return null;
        }
    }

    /** 加密；无密钥/异常时原样返回 */
    public static String encrypt(String plain)
    {
        if (plain == null || plain.isEmpty())
        {
            return plain;
        }
        byte[] key = secretKey();
        if (key == null)
        {
            return plain;
        }
        try
        {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        }
        catch (Exception e)
        {
            log.warn("AES 加密失败，降级明文存储：{}", e.getMessage());
            return plain;
        }
    }

    /** 解密；无前缀/无密钥/解密失败时原样返回 */
    public static String decrypt(String stored)
    {
        if (stored == null || stored.isEmpty() || !stored.startsWith(PREFIX))
        {
            return stored; // 明文或空，直接返回
        }
        byte[] key = secretKey();
        if (key == null)
        {
            return stored; // 无密钥无法解密，原样返回（后台改存时会重新加密）
        }
        try
        {
            byte[] raw = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (raw.length < IV_LEN + 16)
            {
                return stored;
            }
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[raw.length - IV_LEN];
            System.arraycopy(raw, 0, iv, 0, IV_LEN);
            System.arraycopy(raw, IV_LEN, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            log.warn("AES 解密失败（密钥可能已变更），原样返回：{}", e.getMessage());
            return stored;
        }
    }
}
