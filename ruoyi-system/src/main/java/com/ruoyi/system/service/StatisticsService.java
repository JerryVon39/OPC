package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.util.ConfigUtil;

/**
 * 统计查询服务：数据看板 + 热门图书/读者排行
 *
 * 结果缓存到 Redis 5 分钟：前台每次打开页面都请求统计接口，
 * 全表 GROUP BY 实时算属于"每次重复算同一个答案"。
 * 借书/还书/收款/下单/图书读者变动时，由各写路径调用 evictAll() 主动失效，
 * 因此缓存永远不会脏，TTL 只是兜底。
 */
@Service
public class StatisticsService
{
    private static final String KEY_TOP_BOOKS = "stats:topBooks";
    private static final String KEY_TOP_READERS = "stats:topReaders";
    private static final String KEY_DASHBOARD = "stats:dashboard";
    private static final int CACHE_MINUTES = 5;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ConfigUtil configUtil;

    /** 热门图书 Top10（缓存）：按借阅次数统计 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> topBooks()
    {
        List<Map<String, Object>> cached = redisCache.getCacheObject(KEY_TOP_BOOKS);
        if (cached != null)
        {
            return cached;
        }
        List<Map<String, Object>> list = borrowRecordMapper.selectTopBooks(new BorrowRecord());
        redisCache.setCacheObject(KEY_TOP_BOOKS, list, CACHE_MINUTES, TimeUnit.MINUTES);
        return list;
    }

    /** 读者借阅排行 Top10（缓存） */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> topReaders()
    {
        List<Map<String, Object>> cached = redisCache.getCacheObject(KEY_TOP_READERS);
        if (cached != null)
        {
            return cached;
        }
        List<Map<String, Object>> list = borrowRecordMapper.selectTopReaders(new BorrowRecord());
        redisCache.setCacheObject(KEY_TOP_READERS, list, CACHE_MINUTES, TimeUnit.MINUTES);
        return list;
    }

    /** 数据看板聚合统计（缓存）：图书/读者/借阅/订单/罚款 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> dashboard()
    {
        Map<String, Object> cached = redisCache.getCacheObject(KEY_DASHBOARD);
        if (cached != null)
        {
            return cached;
        }
        Map<String, Object> map = borrowRecordMapper.selectDashboard();
        // 库存预警（在架书且库存 <= book.stock.warn 阈值，按库存升序取前 10）：后台首页提醒补货
        int warn = configUtil.getInt("book.stock.warn", 3);
        java.util.List<Book> lowStock = bookMapper.selectLowStockBooks(warn, 10);
        map.put("lowStockBooks", lowStock);
        redisCache.setCacheObject(KEY_DASHBOARD, map, CACHE_MINUTES, TimeUnit.MINUTES);
        return map;
    }

    /** 写路径调用：统计涉及的数据变了，立即失效全部统计缓存（下次请求重新查库） */
    public void evictAll()
    {
        redisCache.deleteObject(KEY_TOP_BOOKS);
        redisCache.deleteObject(KEY_TOP_READERS);
        redisCache.deleteObject(KEY_DASHBOARD);
    }
}
