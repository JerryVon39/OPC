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

/**
 * 图书预约Controller
 */
@RestController
@RequestMapping("/system/reserve")
public class BookReserveController extends BaseController
{
    @Autowired
    private IBookReserveService bookReserveService;

    /** 后台预约列表 */
    @PreAuthorize("@ss.hasPermi('system:borrow:list')")
    @GetMapping("/list")
    public TableDataInfo list(BookReserve bookReserve)
    {
        startPage();
        List<BookReserve> list = bookReserveService.selectBookReserveList(bookReserve);
        return getDataTable(list);
    }

    /** 前台预约（匿名）：按证号预约（仅库存为0可预约） */
    @Anonymous
    @PostMapping("/add")
    public AjaxResult add(String cardNo, Long bookId)
    {
        return toAjax(bookReserveService.reserveByCard(cardNo, bookId));
    }

    /** 前台我的预约（匿名） */
    @Anonymous
    @GetMapping("/myList")
    public AjaxResult myList(String cardNo)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            return error("请输入借书证号");
        }
        return success(bookReserveService.selectReservesByCard(cardNo.trim()));
    }

    /** 前台取消预约（匿名） */
    @Anonymous
    @PostMapping("/cancel")
    public AjaxResult cancel(String cardNo, Long reserveId)
    {
        return toAjax(bookReserveService.cancelByCard(cardNo, reserveId));
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
