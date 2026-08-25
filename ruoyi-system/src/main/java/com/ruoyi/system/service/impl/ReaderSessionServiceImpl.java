package com.ruoyi.system.service.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.ReaderSession;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ReaderSessionService;

/**
 * 成员端会话实现：Redis 存储，14 天滑动续期（sys_config reader.session.minutes 可配），
 * by-card 索引支持多端列表/退出其他设备；登录失败频控保留。
 */
@Service
public class ReaderSessionServiceImpl implements ReaderSessionService
{
    private static final String PREFIX = "reader:session:";
    /** 成员 → 会话集合索引（多端管理用） */
    private static final String CARD_INDEX_PREFIX = "reader:session:by-card:";

    /** 默认会话时长（分钟）＝ 14 天 */
    private static final int DEFAULT_SESSION_MINUTES = 20160;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 登录/补办失败频控：前缀 + key（如 login:IP:证号 / reissue:IP） */
    private static final String FAIL_PREFIX = "reader:fail:";
    private static final int FAIL_LIMIT = 5;
    private static final int FAIL_WINDOW_MINUTES = 30;

    /** 失败递增退避：reader:backoff:{key}，档位 1/2/5/15 分钟（按累计失败次数升档） */
    private static final String BACKOFF_PREFIX = "reader:backoff:";

    /** IP 全局限速：reader:ip-rate:{ip}，60 秒窗口 10 次 */
    private static final String IP_RATE_PREFIX = "reader:ip-rate:";
    private static final int IP_RATE_LIMIT = 10;
    private static final int IP_RATE_WINDOW_SECONDS = 60;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    /** 会话时长（分钟）：sys_config reader.session.minutes，异常回退 14 天 */
    private int sessionMinutes()
    {
        try
        {
            String v = configService.selectConfigByKey("reader.session.minutes");
            if (v != null && !v.isEmpty())
            {
                int m = Integer.parseInt(v);
                if (m >= 5)
                {
                    return m;
                }
            }
        }
        catch (Exception ignore) { }
        return DEFAULT_SESSION_MINUTES;
    }

    @Override
    public String create(String cardNo)
    {
        return create(cardNo, "", "");
    }

    @Override
    public String create(String cardNo, String ip, String device)
    {
        String token;
        do
        {
            token = Long.toUnsignedString(RANDOM.nextLong(), 36)
                    + Long.toUnsignedString(RANDOM.nextLong(), 36);
        }
        while (redisCache.hasKey(PREFIX + token));
        long now = System.currentTimeMillis();
        ReaderSession s = new ReaderSession(cardNo, device == null ? "" : device, ip, now);
        redisCache.setCacheObject(PREFIX + token, s.toStored(), sessionMinutes(), TimeUnit.MINUTES);
        // 多端索引：成员 → 会话集合（RedisCache 无单元素 Set 操作，整体读写）
        String idxKey = CARD_INDEX_PREFIX + cardNo;
        java.util.Set<String> idx = redisCache.getCacheSet(idxKey);
        if (idx == null)
        {
            idx = new java.util.HashSet<>();
        }
        // 顺手清理已过期会话的残留 token（防索引无界增长）
        pruneStaleIndex(idxKey, idx);
        idx.add(token);
        redisCache.setCacheSet(idxKey, idx);
        // 索引本身带 TTL（随会话滑动续期刷新，见 resolveInfo），避免长期残留
        redisCache.expire(idxKey, sessionMinutes(), TimeUnit.MINUTES);
        return token;
    }

    /** 解析（滑动续期）：每次成功解析都刷新 TTL */
    @Override
    public String resolve(String token)
    {
        ReaderSession s = resolveInfo(token);
        return s == null ? null : s.getCardNo();
    }

    @Override
    public ReaderSession resolveInfo(String token)
    {
        if (token == null || token.trim().isEmpty())
        {
            return null;
        }
        String key = PREFIX + token.trim();
        String stored = redisCache.getCacheObject(key);
        ReaderSession s = ReaderSession.fromStored(stored);
        if (s == null)
        {
            return null;
        }
        // 滑动续期 + 刷新最近活跃时间（值重写，TTL 一并重置）
        s.setLastActiveAt(System.currentTimeMillis());
        redisCache.setCacheObject(key, s.toStored(), sessionMinutes(), TimeUnit.MINUTES);
        // 索引 TTL 随会话续期刷新，避免活跃成员的索引过期丢失
        redisCache.expire(CARD_INDEX_PREFIX + s.getCardNo(), sessionMinutes(), TimeUnit.MINUTES);
        return s;
    }

    /** 从成员索引中剔除已过期（会话 key 不存在）的残留 token */
    private void pruneStaleIndex(String idxKey, java.util.Set<String> idx)
    {
        if (idx.isEmpty())
        {
            return;
        }
        java.util.Iterator<String> it = idx.iterator();
        while (it.hasNext())
        {
            if (!redisCache.hasKey(PREFIX + it.next()))
            {
                it.remove();
            }
        }
        redisCache.setCacheSet(idxKey, idx);
    }

    @Override
    public List<ReaderSession> listSessions(String cardNo)
    {
        List<ReaderSession> result = new ArrayList<>();
        Set<String> tokens = redisCache.getCacheSet(CARD_INDEX_PREFIX + cardNo);
        if (tokens == null)
        {
            return result;
        }
        for (String token : tokens)
        {
            ReaderSession s = ReaderSession.fromStored(redisCache.getCacheObject(PREFIX + token));
            if (s != null)
            {
                result.add(s);
            }
        }
        // 最近活跃倒序（越新越靠前）
        result.sort(Comparator.comparingLong(ReaderSession::getLastActiveAt).reversed());
        return result;
    }

    @Override
    public int revokeOthers(String cardNo, String currentToken)
    {
        int removed = 0;
        String idxKey = CARD_INDEX_PREFIX + cardNo;
        Set<String> tokens = redisCache.getCacheSet(idxKey);
        if (tokens == null)
        {
            return 0;
        }
        java.util.Set<String> keep = new java.util.HashSet<>();
        for (String token : tokens)
        {
            if (currentToken != null && token.equals(currentToken))
            {
                keep.add(token); // 保留当前设备；currentToken 为 null 时全部退出（重置密码场景）
                continue;
            }
            redisCache.deleteObject(PREFIX + token);
            removed++;
        }
        redisCache.setCacheSet(idxKey, keep);
        return removed;
    }

    @Override
    public void remove(String token)
    {
        if (token == null || token.trim().isEmpty())
        {
            return;
        }
        String key = PREFIX + token.trim();
        ReaderSession s = ReaderSession.fromStored(redisCache.getCacheObject(key));
        redisCache.deleteObject(key);
        if (s != null)
        {
            String idxKey = CARD_INDEX_PREFIX + s.getCardNo();
            Set<String> idx = redisCache.getCacheSet(idxKey);
            if (idx != null)
            {
                idx.remove(token.trim());
                redisCache.setCacheSet(idxKey, idx);
            }
        }
    }

    @Override
    public boolean isBlocked(String key)
    {
        Integer n = redisCache.getCacheObject(FAIL_PREFIX + key);
        if (n != null && n >= FAIL_LIMIT)
        {
            return true; // 5 次/30 分钟锁定
        }
        return redisCache.hasKey(BACKOFF_PREFIX + key); // 递增退避期内同样拦截
    }

    @Override
    public void recordFail(String key)
    {
        String k = FAIL_PREFIX + key;
        Integer n = redisCache.getCacheObject(k);
        if (n == null)
        {
            // 首次失败：开启 30 分钟窗口 + 1 分钟退避
            redisCache.setCacheObject(k, 1, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
            redisCache.setCacheObject(BACKOFF_PREFIX + key, 1, 1, TimeUnit.MINUTES);
        }
        else
        {
            // 窗口内叠加：必须带剩余 TTL 重设（Redis SET 不带过期参数会清除原 TTL，导致计数永不过期）
            int count = n + 1;
            Long ttl = redisCache.getExpire(k);
            if (ttl != null && ttl > 0)
            {
                redisCache.setCacheObject(k, count, ttl.intValue(), TimeUnit.SECONDS);
            }
            else
            {
                redisCache.setCacheObject(k, count, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
            }
            int backoffSeconds = count < 2 ? 60 : count < 4 ? 120 : count < 6 ? 300 : 900;
            redisCache.setCacheObject(BACKOFF_PREFIX + key, 1, backoffSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void clearFail(String key)
    {
        redisCache.deleteObject(FAIL_PREFIX + key);
        redisCache.deleteObject(BACKOFF_PREFIX + key);
    }

    @Override
    public boolean isIpRateLimited(String ip)
    {
        if (ip == null || ip.isEmpty())
        {
            return false;
        }
        Integer n = redisCache.getCacheObject(IP_RATE_PREFIX + ip);
        return n != null && n >= IP_RATE_LIMIT;
    }

    @Override
    public void recordIpRequest(String ip)
    {
        if (ip == null || ip.isEmpty())
        {
            return;
        }
        String k = IP_RATE_PREFIX + ip;
        Integer n = redisCache.getCacheObject(k);
        if (n == null)
        {
            redisCache.setCacheObject(k, 1, IP_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        else
        {
            // 窗口内叠加：必须带剩余 TTL 重设（Redis SET 不带过期参数会清除原 TTL，导致计数永不过期、IP 被永久封禁）
            int count = n + 1;
            Long ttl = redisCache.getExpire(k);
            if (ttl != null && ttl > 0)
            {
                redisCache.setCacheObject(k, count, ttl.intValue(), TimeUnit.SECONDS);
            }
            else
            {
                redisCache.setCacheObject(k, count, IP_RATE_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        }
    }
}
