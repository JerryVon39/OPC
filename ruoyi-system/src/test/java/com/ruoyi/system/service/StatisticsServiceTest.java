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
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.util.ConfigUtil;

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

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ConfigUtil configUtil;

    @Mock
    private com.ruoyi.system.mapper.CmsArticleMapper cmsArticleMapper;

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

    /** dashboard 缓存未命中：查库写入，且带库存预警列表 */
    @Test
    void dashboard_cacheMiss_queriesAndCaches()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("bookTotal", 100);
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        when(borrowRecordMapper.selectDashboard()).thenReturn(map);
        when(configUtil.getInt("book.stock.warn", 3)).thenReturn(3);
        when(bookMapper.selectLowStockBooks(3, 10)).thenReturn(new ArrayList<Book>());
        // CMS 文章维度（2026-08-27 工作台数据卡加入后测试同步）
        java.util.Map<String, Object> cmsStats = new HashMap<>();
        cmsStats.put("articleTotal", 15);
        when(cmsArticleMapper.selectCmsArticleStats()).thenReturn(cmsStats);

        Map<String, Object> result = statisticsService.dashboard();
        assertEquals(map, result);
        assertNotNull(result.get("lowStockBooks"));
        assertEquals(cmsStats, result.get("cmsArticle"));
        verify(borrowRecordMapper).selectDashboard();
        verify(bookMapper).selectLowStockBooks(3, 10);
        verify(cmsArticleMapper).selectCmsArticleStats();
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
