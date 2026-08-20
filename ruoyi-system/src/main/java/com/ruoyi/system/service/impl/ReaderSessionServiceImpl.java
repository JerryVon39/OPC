package com.ruoyi.system.service.impl;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.ReaderSessionService;

@Service
public class ReaderSessionServiceImpl implements ReaderSessionService
{
    private static final String PREFIX = "reader:session:";
    private static final int SESSION_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 登录/补办失败频控：前缀 + key（如 login:IP:证号 / reissue:IP） */
    private static final String FAIL_PREFIX = "reader:fail:";
    private static final int FAIL_LIMIT = 5;
    private static final int FAIL_WINDOW_MINUTES = 30;

    @Autowired
    private RedisCache redisCache;

    @Override
    public String create(String cardNo)
    {
        String token;
        do
        {
            token = Long.toUnsignedString(RANDOM.nextLong(), 36)
                    + Long.toUnsignedString(RANDOM.nextLong(), 36);
        }
        while (redisCache.hasKey(PREFIX + token));
        redisCache.setCacheObject(PREFIX + token, cardNo, SESSION_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public String resolve(String token)
    {
        if (token == null || token.trim().isEmpty())
        {
            return null;
        }
        return redisCache.getCacheObject(PREFIX + token.trim());
    }

    @Override
    public void remove(String token)
    {
        if (token != null && !token.trim().isEmpty())
        {
            redisCache.deleteObject(PREFIX + token.trim());
        }
    }

    @Override
    public boolean isBlocked(String key)
    {
        Integer n = redisCache.getCacheObject(FAIL_PREFIX + key);
        return n != null && n >= FAIL_LIMIT;
    }

    @Override
    public void recordFail(String key)
    {
        String k = FAIL_PREFIX + key;
        Integer n = redisCache.getCacheObject(k);
        if (n == null)
        {
            // 首次失败：开启 30 分钟窗口
            redisCache.setCacheObject(k, 1, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        else
        {
            // 窗口内叠加（setCacheObject 不带过期参数不会重置已有 TTL）
            redisCache.setCacheObject(k, n + 1);
        }
    }

    @Override
    public void clearFail(String key)
    {
        redisCache.deleteObject(FAIL_PREFIX + key);
    }
}
