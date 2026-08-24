package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.service.IBorrowRecordService;
import com.ruoyi.system.service.ReaderSessionService;
import com.ruoyi.system.service.StatisticsService;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 报名记录Controller
 */
@RestController
@RequestMapping("/system/borrow")
public class BorrowRecordController extends BaseController
{
    @Autowired
    private IBorrowRecordService borrowRecordService;

    @Autowired
    private ReaderSessionService readerSessionService;

    @Autowired
    private StatisticsService statisticsService;

    /** 导出报名记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:export')")
    @Log(title = "报名记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BorrowRecord borrowRecord)
    {
        List<BorrowRecord> list = borrowRecordService.selectBorrowRecordList(borrowRecord);
        ExcelUtil<BorrowRecord> util = new ExcelUtil<BorrowRecord>(BorrowRecord.class);
        util.exportExcel(response, list, "报名记录数据");
    }

    /** 查询报名记录列表 */
    @PreAuthorize("@ss.hasPermi('system:borrow:list')")
    @GetMapping("/list")
    public TableDataInfo list(BorrowRecord borrowRecord)
    {
        startPage();
        List<BorrowRecord> list = borrowRecordService.selectBorrowRecordList(borrowRecord);
        return getDataTable(list);
    }

    /** 获取报名记录详细信息 */
    @PreAuthorize("@ss.hasPermi('system:borrow:query')")
    @GetMapping(value = "/{borrowId}")
    public AjaxResult getInfo(@PathVariable("borrowId") Long borrowId)
    {
        return success(borrowRecordService.selectBorrowRecordByBorrowId(borrowId));
    }

    /** 新增报名记录（报名：自动校验库存并减1） */
    @PreAuthorize("@ss.hasPermi('system:borrow:add')")
    @Log(title = "报名记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BorrowRecord borrowRecord)
    {
        return toAjax(borrowRecordService.insertBorrowRecord(borrowRecord));
    }

    /** 修改报名记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "报名记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BorrowRecord borrowRecord)
    {
        return toAjax(borrowRecordService.updateBorrowRecord(borrowRecord));
    }

    /** 前台报名：短期成员会话 + 证号兼容校验 */
    @Anonymous
    @PostMapping("/borrowByCard")
    public AjaxResult borrowByCard(String cardNo, String sessionToken, Long bookId, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(borrowRecordService.borrowByCard(sessionCard, bookId));
    }

    /** 续借：截止日期 +30 天 */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "报名记录", businessType = BusinessType.UPDATE)
    @PutMapping("/renew/{borrowId}")
    public AjaxResult renew(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.renewBook(borrowId));
    }

    /** 前台"我的报名"：短期成员会话查询 */
    @Anonymous
    @GetMapping("/queryByCard")
    public AjaxResult queryByCard(String cardNo, String sessionToken, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return success(borrowRecordService.selectBorrowListByCard(sessionCard));
    }

    /** 前台续借：短期成员会话 + 记录归属校验 */
    @Anonymous
    @PostMapping("/renewByCard")
    public AjaxResult renewByCard(String cardNo, String sessionToken, Long borrowId, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(borrowRecordService.renewByCard(sessionCard, borrowId));
    }

    /** 报名统计·热门服务（匿名：前台热门推荐用，数据走 Redis 缓存）
     * 注意：成员排行（含成员编号）不在此匿名接口下发——姓名+证号即前台登录凭证，
     * 明文公开等同把成员账户挂在公网上；管理端走下方 /stats/readers 权限接口 */
    @Anonymous
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("topBooks", statisticsService.topBooks());
        return success(result);
    }

    /** 报名统计·成员排行 Top10（管理端：含证号，需报名统计权限） */
    @PreAuthorize("@ss.hasPermi('system:borrow:stats')")
    @GetMapping("/stats/readers")
    public AjaxResult statsReaders()
    {
        return success(statisticsService.topReaders());
    }

    /** 完成：恢复库存 + 自动结算逾期罚款 */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "报名记录", businessType = BusinessType.UPDATE)
    @PutMapping("/return/{borrowId}")
    public AjaxResult returnBook(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.returnBook(borrowId));
    }

    /** 罚款收款：缴纳逾期罚款（收银台操作） */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "报名记录", businessType = BusinessType.UPDATE)
    @PutMapping("/payFine/{borrowId}")
    public AjaxResult payFine(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.payFine(borrowId));
    }

    /** 删除报名记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:remove')")
    @Log(title = "报名记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{borrowIds}")
    public AjaxResult remove(@PathVariable Long[] borrowIds)
    {
        return toAjax(borrowRecordService.deleteBorrowRecordByBorrowIds(borrowIds));
    }
}
