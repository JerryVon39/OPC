package com.ruoyi.system.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.BookPurchaseReqMapper;
import com.ruoyi.system.mapper.CmsArticleMapper;
import com.ruoyi.system.mapper.CmsBlockMapper;
import com.ruoyi.system.service.StatisticsService;

/**
 * 首页数据看板Controller
 *
 * 后台首页展示业务统计：服务/成员/报名/订单 + 热门服务 Top
 */
@RestController
@RequestMapping("/system/dashboard")
public class DashboardController extends BaseController
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private CmsArticleMapper cmsArticleMapper;

    @Autowired
    private CmsBlockMapper cmsBlockMapper;

    @Autowired
    private BookPurchaseReqMapper bookPurchaseReqMapper;

    /** 业务统计（登录即可访问，供后台首页看板使用）：数据走 Redis 缓存（5分钟） */
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> stats = statisticsService.dashboard();
        // 热门服务 Top10（前端取前5展示）
        stats.put("topBooks", statisticsService.topBooks());
        // 运营待办：待审核入驻申请数（看板"待办"卡片用；草稿/回收站数在 dashboard() 的 cmsArticle 内）
        stats.put("pendingApplyCount", bookPurchaseReqMapper.countPendingApply());
        return success(stats);
    }

    /** 前台公开统计（匿名）：店铺数据条用（服务/成员/今日报名/今日订单，无敏感数据） */
    @Anonymous
    @GetMapping("/publicStats")
    public AjaxResult publicStats()
    {
        Map<String, Object> stats = statisticsService.dashboard();
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("bookTotal", stats.get("bookTotal"));
        result.put("readerTotal", stats.get("readerTotal"));
        result.put("borrowToday", stats.get("borrowToday"));
        result.put("orderToday", stats.get("orderToday"));
        return success(result);
    }

    /** 最近编辑记录（运营工作台）：文章/区块各 5 条，点击直达编辑 */
    @GetMapping("/recentEdits")
    public AjaxResult recentEdits()
    {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("articles", cmsArticleMapper.selectRecentArticles(5));
        result.put("blocks", cmsBlockMapper.selectRecentBlocks(5));
        return success(result);
    }
}
