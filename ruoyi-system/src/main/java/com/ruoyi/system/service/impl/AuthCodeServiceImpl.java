package com.ruoyi.system.service.impl;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.AuthCodeService;
import com.ruoyi.system.service.VerificationSender;

/**
 * 验证码服务实现：Redis 存储 + 频控（目标 60s / IP 每日 10 条）
 */
@Service
public class AuthCodeServiceImpl implements AuthCodeService
{
    /** 验证码本体：auth:code:{purpose}:{target} */
    private static final String PREFIX = "auth:code:";
    /** 发送间隔闸：auth:code:throttle:{target}（按邮箱全局，不含 purpose——防轮换 purpose 绕过 60s 节流） */
    private static final String THROTTLE_PREFIX = "auth:code:throttle:";
    /** IP 每日计数：auth:code:ip:{ip}（按 IP 全局，不含 purpose——防轮换 purpose 绕过每日配额） */
    private static final String IP_PREFIX = "auth:code:ip:";
    /** 校验失败计数：auth:code:try:{purpose}:{target}（连续失败 5 次作废验证码，防暴力枚举） */
    private static final String TRY_PREFIX = "auth:code:try:";
    private static final int TRY_LIMIT = 5;

    private static final int TTL_MINUTES = 15;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int DAILY_IP_LIMIT = 10;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private VerificationSender verificationSender;

    @Override
    public void sendCode(String target, String purpose, String ip)
    {
        if (target == null || target.trim().isEmpty())
        {
            throw new ServiceException("目标邮箱不能为空");
        }
        String t = target.trim().toLowerCase();
        // 频控 1：同目标（邮箱）60 秒内只能发一条，与 purpose 无关（防轮换 purpose 绕过）
        if (redisCache.hasKey(THROTTLE_PREFIX + t))
        {
            throw new ServiceException("验证码发送过于频繁，请 60 秒后再试");
        }
        // 频控 2：同 IP 每日上限，与 purpose 无关（防轮换 purpose 绕过配额批量轰炸）
        String ipKey = IP_PREFIX + (ip == null ? "unknown" : ip);
        Integer sent = redisCache.getCacheObject(ipKey);
        if (sent != null && sent >= DAILY_IP_LIMIT)
        {
            throw new ServiceException("今日验证码发送次数已达上限，请明天再试或联系管理员");
        }
        // 生成 6 位数字验证码（SecureRandom，不可预测）
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        redisCache.setCacheObject(PREFIX + purpose + ":" + t, code, TTL_MINUTES, TimeUnit.MINUTES);
        redisCache.setCacheObject(THROTTLE_PREFIX + t, 1, SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (sent == null)
        {
            redisCache.setCacheObject(ipKey, 1, 24, TimeUnit.HOURS);
        }
        else
        {
            // 叠加必须带剩余 TTL 重设（Redis SET 不带过期参数会清除原 TTL，导致每日计数永不过期）
            Long ttl = redisCache.getExpire(ipKey);
            if (ttl != null && ttl > 0)
            {
                redisCache.setCacheObject(ipKey, sent + 1, ttl.intValue(), TimeUnit.SECONDS);
            }
            else
            {
                redisCache.setCacheObject(ipKey, sent + 1, 24, TimeUnit.HOURS);
            }
        }
        verificationSender.send(t, code, TTL_MINUTES);
    }

    @Override
    public boolean verify(String target, String purpose, String code)
    {
        if (target == null || target.trim().isEmpty() || code == null || code.trim().isEmpty())
        {
            return false;
        }
        String t = target.trim().toLowerCase();
        String key = PREFIX + purpose + ":" + t;
        String stored = redisCache.getCacheObject(key);
        if (stored == null)
        {
            return false; // 无验证码/已作废
        }
        if (!stored.equals(code.trim()))
        {
            // 失败计数：连续失败 5 次作废（防暴力枚举 6 位验证码）；计数窗口与验证码有效期一致
            String tryKey = TRY_PREFIX + purpose + ":" + t;
            Integer tries = redisCache.getCacheObject(tryKey);
            int n = tries == null ? 1 : tries + 1;
            Long ttl = redisCache.getExpire(key);
            int remain = (ttl != null && ttl > 0) ? ttl.intValue() : TTL_MINUTES * 60;
            if (n >= TRY_LIMIT)
            {
                redisCache.deleteObject(key);
                redisCache.deleteObject(tryKey);
            }
            else
            {
                redisCache.setCacheObject(tryKey, n, remain, TimeUnit.SECONDS);
            }
            return false;
        }
        // 一次性：验证通过立即作废
        redisCache.deleteObject(key);
        redisCache.deleteObject(TRY_PREFIX + purpose + ":" + t);
        return true;
    }
}
