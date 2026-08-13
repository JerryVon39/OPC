package com.ruoyi.system.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;

/**
 * 首页数据看板Controller
 *
 * 后台首页展示业务统计：图书/读者/借阅/订单 + 热门图书 Top
 */
@RestController
@RequestMapping("/system/dashboard")
public class DashboardController extends BaseController
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    /** 业务统计（登录即可访问，供后台首页看板使用） */
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> stats = borrowRecordMapper.selectDashboard();
        // 热门图书 Top10（前端取前5展示）
        stats.put("topBooks", borrowRecordMapper.selectTopBooks(new BorrowRecord()));
        return success(stats);
    }
}
