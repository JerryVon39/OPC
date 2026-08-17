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
}
