package com.ruoyi.system.service.impl;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.ReaderSession;
import com.ruoyi.system.service.ISysConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 读者会话 + 登录/补办频控单测：纯 Mockito 不依赖 Spring/Redis */
@ExtendWith(MockitoExtension.class)
class ReaderSessionServiceImplTest
{
    @Mock
    private RedisCache redisCache;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private ReaderSessionServiceImpl sessionService;

    /** 会话创建：token 非空且写入 Redis（默认 14 天 TTL；sys_config 可配） */
    @Test
    void create_storesTokenWithTtl()
    {
        // token 生成去重检查：hasKey 返回 false 直接通过
        when(redisCache.hasKey(anyString())).thenReturn(false);
        String token = sessionService.create("JS12345678");
        assertTrue(token != null && !token.trim().isEmpty(), "token 不应为空");
        // configService 未打桩 → sessionMinutes() 回退默认 20160（14 天）
        verify(redisCache).setCacheObject(anyString(), any(), eq(20160), eq(TimeUnit.MINUTES));
    }

    /** 会话解析：命中返回证号，未命中返回 null（解析成功会滑动续期重写值，桩用合法存储格式） */
    @Test
    void resolve_returnsCardOrNull()
    {
        when(redisCache.getCacheObject("reader:session:abc"))
                .thenReturn(new ReaderSession("JS12345678", "", "", 0).toStored());
        assertEquals("JS12345678", sessionService.resolve("abc"));
        assertNull(sessionService.resolve("missing"));
        assertNull(sessionService.resolve(null));
        assertNull(sessionService.resolve("  "));
    }

    /** 会话删除：空 token 不操作 Redis */
    @Test
    void remove_ignoresBlankToken()
    {
        sessionService.remove(null);
        sessionService.remove(" ");
        verify(redisCache, never()).deleteObject(anyString());
    }

    /** 频控：失败次数 < 上限不拦截，达到上限拦截 */
    @Test
    void isBlocked_threshold()
    {
        when(redisCache.getCacheObject("reader:fail:login:127.0.0.1:JS1")).thenReturn(4);
        assertFalse(sessionService.isBlocked("login:127.0.0.1:JS1"), "4 次不应拦截");

        when(redisCache.getCacheObject("reader:fail:login:127.0.0.1:JS2")).thenReturn(5);
        assertTrue(sessionService.isBlocked("login:127.0.0.1:JS2"), "5 次应拦截");
    }

    /** 频控：首次失败开启 30 分钟窗口，后续失败叠加计数且不重置 TTL */
    @Test
    void recordFail_countsAndKeepsTtl()
    {
        when(redisCache.getCacheObject("reader:fail:login:127.0.0.1:JS1")).thenReturn(null);
        sessionService.recordFail("login:127.0.0.1:JS1");
        verify(redisCache).setCacheObject("reader:fail:login:127.0.0.1:JS1", 1, 30, TimeUnit.MINUTES);

        when(redisCache.getCacheObject("reader:fail:login:127.0.0.1:JS1")).thenReturn(3);
        // 窗口内叠加：按剩余 TTL 重设（防计数永不过期），并升档退避
        when(redisCache.getExpire("reader:fail:login:127.0.0.1:JS1")).thenReturn(1200L);
        sessionService.recordFail("login:127.0.0.1:JS1");
        verify(redisCache).setCacheObject("reader:fail:login:127.0.0.1:JS1", 4, 1200, TimeUnit.SECONDS);
    }

    /** 频控：成功后清除计数 */
    @Test
    void clearFail_deletesKey()
    {
        sessionService.clearFail("login:127.0.0.1:JS1");
        verify(redisCache).deleteObject("reader:fail:login:127.0.0.1:JS1");
    }
}
