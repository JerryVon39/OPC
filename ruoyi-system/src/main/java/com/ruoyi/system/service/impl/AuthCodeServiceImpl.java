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
    /** 发送间隔闸：auth:code:throttle:{purpose}:{target} */
    private static final String THROTTLE_PREFIX = "auth:code:throttle:";
    /** IP 每日计数：auth:code:ip:{purpose}:{ip} */
    private static final String IP_PREFIX = "auth:code:ip:";

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
        // 频控 1：同目标 60 秒内只能发一条
        if (redisCache.hasKey(THROTTLE_PREFIX + purpose + ":" + t))
        {
            throw new ServiceException("验证码发送过于频繁，请 60 秒后再试");
        }
        // 频控 2：同 IP 每日上限（防批量轰炸）
        String ipKey = IP_PREFIX + purpose + ":" + (ip == null ? "unknown" : ip);
        Integer sent = redisCache.getCacheObject(ipKey);
        if (sent != null && sent >= DAILY_IP_LIMIT)
        {
            throw new ServiceException("今日验证码发送次数已达上限，请明天再试或联系管理员");
        }
        // 生成 6 位数字验证码（SecureRandom，不可预测）
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        redisCache.setCacheObject(PREFIX + purpose + ":" + t, code, TTL_MINUTES, TimeUnit.MINUTES);
        redisCache.setCacheObject(THROTTLE_PREFIX + purpose + ":" + t, 1, SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (sent == null)
        {
            redisCache.setCacheObject(ipKey, 1, 24, TimeUnit.HOURS);
        }
        else
        {
            redisCache.setCacheObject(ipKey, sent + 1);
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
        if (stored == null || !stored.equals(code.trim()))
        {
            return false;
        }
        // 一次性：验证通过立即作废
        redisCache.deleteObject(key);
        return true;
    }
}
