package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 借阅记录Controller
 */
@RestController
@RequestMapping("/system/borrow")
public class BorrowRecordController extends BaseController
{
    @Autowired
    private IBorrowRecordService borrowRecordService;

    /** 查询借阅记录列表 */
    @GetMapping("/list")
    public TableDataInfo list(BorrowRecord borrowRecord)
    {
        startPage();
        List<BorrowRecord> list = borrowRecordService.selectBorrowRecordList(borrowRecord);
        return getDataTable(list);
    }

    /** 获取借阅记录详细信息 */
    @GetMapping(value = "/{borrowId}")
    public AjaxResult getInfo(@PathVariable("borrowId") Long borrowId)
    {
        return success(borrowRecordService.selectBorrowRecordByBorrowId(borrowId));
    }

    /** 新增借阅记录（借书：自动校验库存并减1） */
    @Log(title = "借阅记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BorrowRecord borrowRecord)
    {
        return toAjax(borrowRecordService.insertBorrowRecord(borrowRecord));
    }

    /** 修改借阅记录 */
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BorrowRecord borrowRecord)
    {
        return toAjax(borrowRecordService.updateBorrowRecord(borrowRecord));
    }

    /** 前台借书（匿名公开接口）：按借书证号借书 */
    @Anonymous
    @PostMapping("/borrowByCard")
    public AjaxResult borrowByCard(String cardNo, Long bookId)
    {
        return toAjax(borrowRecordService.borrowByCard(cardNo, bookId));
    }

    /** 续借：应还日期 +30 天 */
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping("/renew/{borrowId}")
    public AjaxResult renew(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.renewBook(borrowId));
    }

    /** 前台"我的借阅"：按借书证号查询（匿名公开接口） */
    @Anonymous
    @GetMapping("/queryByCard")
    public AjaxResult queryByCard(String cardNo)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            return error("请输入借书证号");
        }
        return success(borrowRecordService.selectBorrowListByCard(cardNo.trim()));
    }

    /** 借阅统计：热门图书 + 读者排行 */
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("topBooks", borrowRecordService.selectTopBooks());
        result.put("topReaders", borrowRecordService.selectTopReaders());
        return success(result);
    }

    /** 还书：恢复库存 */
    @Log(title = "借阅记录", businessType = BusinessType.UPDATE)
    @PutMapping("/return/{borrowId}")
    public AjaxResult returnBook(@PathVariable("borrowId") Long borrowId)
    {
        return toAjax(borrowRecordService.returnBook(borrowId));
    }

    /** 删除借阅记录 */
    @Log(title = "借阅记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{borrowIds}")
    public AjaxResult remove(@PathVariable Long[] borrowIds)
    {
        return toAjax(borrowRecordService.deleteBorrowRecordByBorrowIds(borrowIds));
    }
}
