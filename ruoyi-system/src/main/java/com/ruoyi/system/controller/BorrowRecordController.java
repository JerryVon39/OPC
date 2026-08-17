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
 * 借阅记录Controller
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

    /** 导出借阅记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:export')")
    @Log(title = "借阅记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BorrowRecord borrowRecord)
    {
        List<BorrowRecord> list = borrowRecordService.selectBorrowRecordList(borrowRecord);
        ExcelUtil<BorrowRecord> util = new ExcelUtil<BorrowRecord>(BorrowRecord.class);
        util.exportExcel(response, list, "借阅记录数据");
    }

    /** 查询借阅记录列表 */
    @PreAuthorize("@ss.hasPermi('system:borrow:list')")
    @GetMapping("/list")
    public TableDataInfo list(BorrowRecord borrowRecord)
    {
        startPage();
        List<BorrowRecord> list = borrowRecordService.selectBorrowRecordList(borrowRecord);
        return getDataTable(list);
    }

    /** 获取借阅记录详细信息 */
    @PreAuthorize("@ss.hasPermi('system:borrow:query')")
    @GetMapping(value = "/{borrowId}")
    public AjaxResult getInfo(@PathVariable("borrowId") Long borrowId)
    {
        return success(borrowRecordService.selectBorrowRecordByBorrowId(borrowId));
    }

    /** 新增借阅记录（借书：自动校验库存并减1） */
    @PreAuthorize("@ss.hasPermi('system:borrow:add')")
    @Log(title = "借阅记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BorrowRecord borrowRecord)
    {
        return toAjax(borrowRecordService.insertBorrowRecord(borrowRecord));
    }

    /** 修改借阅记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BorrowRecord borrowRecord)
    {
        return toAjax(borrowRecordService.updateBorrowRecord(borrowRecord));
    }

    /** 前台借书：短期读者会话 + 证号兼容校验 */
    @Anonymous
    @PostMapping("/borrowByCard")
    public AjaxResult borrowByCard(String cardNo, String sessionToken, Long bookId)
    {
        String sessionCard = readerSessionService.resolve(sessionToken);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(borrowRecordService.borrowByCard(sessionCard, bookId));
    }

    /** 续借：应还日期 +30 天 */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping("/renew/{borrowId}")
    public AjaxResult renew(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.renewBook(borrowId));
    }

    /** 前台"我的借阅"：短期读者会话查询 */
    @Anonymous
    @GetMapping("/queryByCard")
    public AjaxResult queryByCard(String cardNo, String sessionToken)
    {
        String sessionCard = readerSessionService.resolve(sessionToken);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return success(borrowRecordService.selectBorrowListByCard(sessionCard));
    }

    /** 前台续借：短期读者会话 + 记录归属校验 */
    @Anonymous
    @PostMapping("/renewByCard")
    public AjaxResult renewByCard(String cardNo, String sessionToken, Long borrowId)
    {
        String sessionCard = readerSessionService.resolve(sessionToken);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(borrowRecordService.renewByCard(sessionCard, borrowId));
    }

    /** 借阅统计：热门图书 + 读者排行（匿名：前台热门推荐用，数据走 Redis 缓存） */
    @Anonymous
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("topBooks", statisticsService.topBooks());
        result.put("topReaders", statisticsService.topReaders());
        return success(result);
    }

    /** 还书：恢复库存 + 自动结算逾期罚款 */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping("/return/{borrowId}")
    public AjaxResult returnBook(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.returnBook(borrowId));
    }

    /** 罚款收款：缴纳逾期罚款（收银台操作） */
    @PreAuthorize("@ss.hasPermi('system:borrow:edit')")
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping("/payFine/{borrowId}")
    public AjaxResult payFine(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.payFine(borrowId));
    }

    /** 删除借阅记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:remove')")
    @Log(title = "借阅记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{borrowIds}")
    public AjaxResult remove(@PathVariable Long[] borrowIds)
    {
        return toAjax(borrowRecordService.deleteBorrowRecordByBorrowIds(borrowIds));
    }
}
