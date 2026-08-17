package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.BookReserve;
import com.ruoyi.system.service.IBookReserveService;
import com.ruoyi.system.service.ReaderSessionService;

/**
 * 图书预约Controller
 */
@RestController
@RequestMapping("/system/reserve")
public class BookReserveController extends BaseController
{
    @Autowired
    private IBookReserveService bookReserveService;

    @Autowired
    private ReaderSessionService readerSessionService;

    /** 后台预约列表 */
    @PreAuthorize("@ss.hasPermi('system:borrow:list')")
    @GetMapping("/list")
    public TableDataInfo list(BookReserve bookReserve)
    {
        startPage();
        List<BookReserve> list = bookReserveService.selectBookReserveList(bookReserve);
        return getDataTable(list);
    }

    /** 前台预约：短期读者会话 + 证号校验 */
    @Anonymous
    @PostMapping("/add")
    public AjaxResult add(String cardNo, String sessionToken, Long bookId)
    {
        String sessionCard = readerSessionService.resolve(sessionToken);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(bookReserveService.reserveByCard(sessionCard, bookId));
    }

    /** 前台我的预约：短期读者会话查询 */
    @Anonymous
    @GetMapping("/myList")
    public AjaxResult myList(String cardNo, String sessionToken)
    {
        String sessionCard = readerSessionService.resolve(sessionToken);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return success(bookReserveService.selectReservesByCard(sessionCard));
    }

    /** 前台取消预约：短期读者会话 + 证号归属校验 */
    @Anonymous
    @PostMapping("/cancel")
    public AjaxResult cancel(String cardNo, String sessionToken, Long reserveId)
    {
        String sessionCard = readerSessionService.resolve(sessionToken);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(bookReserveService.cancelByCard(sessionCard, reserveId));
    }

    /** 后台删除预约记录 */
    @PreAuthorize("@ss.hasPermi('system:borrow:remove')")
    @Log(title = "图书预约", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reserveIds}")
    public AjaxResult remove(@PathVariable Long[] reserveIds)
    {
        return toAjax(bookReserveService.deleteBookReserveByReserveIds(reserveIds));
    }
}
