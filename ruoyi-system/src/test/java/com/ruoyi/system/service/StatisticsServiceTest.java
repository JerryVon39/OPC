package com.ruoyi.system.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 统计缓存单元测试：缓存命中不查库、未命中查库并写入、失效清空
 */
@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest
{
    @Mock
    private RedisCache redisCache;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @InjectMocks
    private StatisticsService statisticsService;

    /** 缓存未命中：查库 + 写入缓存 */
    @Test
    void topBooks_cacheMiss_queriesAndCaches()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        when(borrowRecordMapper.selectTopBooks(any(BorrowRecord.class))).thenReturn(list);

        assertEquals(list, statisticsService.topBooks());
        verify(borrowRecordMapper).selectTopBooks(any(BorrowRecord.class));
        verify(redisCache).setCacheObject(eq("stats:topBooks"), eq(list), anyInt(), any());
    }

    /** 缓存命中：直接返回，不查库 */
    @Test
    void topBooks_cacheHit_noQuery()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        when(redisCache.getCacheObject(anyString())).thenReturn(list);

        assertEquals(list, statisticsService.topBooks());
        verify(borrowRecordMapper, never()).selectTopBooks(any(BorrowRecord.class));
    }

    /** dashboard 缓存未命中：查库写入 */
    @Test
    void dashboard_cacheMiss_queriesAndCaches()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("bookTotal", 100);
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        when(borrowRecordMapper.selectDashboard()).thenReturn(map);

        assertEquals(map, statisticsService.dashboard());
        verify(borrowRecordMapper).selectDashboard();
    }

    /** 失效：三个 key 全部删除 */
    @Test
    void evictAll_deletesAllKeys()
    {
        statisticsService.evictAll();
        verify(redisCache).deleteObject("stats:topBooks");
        verify(redisCache).deleteObject("stats:topReaders");
        verify(redisCache).deleteObject("stats:dashboard");
    }
}
