package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.service.IRecycleService;

/**
 * 回收站Controller：误删服务/成员恢复（还原/彻底删除/清空）
 */
@RestController
@RequestMapping("/system/recycle")
public class RecycleController extends BaseController
{
    @Autowired
    private IRecycleService recycleService;

    // ================= 服务回收站 =================

    @PreAuthorize("@ss.hasPermi('system:recycle:book:list')")
    @GetMapping("/book/list")
    public TableDataInfo bookList(Book query)
    {
        startPage();
        List<Book> list = recycleService.listRecycleBooks(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:book:list')")
    @GetMapping("/book/count")
    public AjaxResult bookCount()
    {
        return success(recycleService.countBooks());
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:book:list')")
    @PutMapping("/book/restore/{recycleIds}")
    public AjaxResult bookRestore(@PathVariable Long[] recycleIds)
    {
        return toAjax(recycleService.restoreBooks(recycleIds));
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:book:list')")
    @DeleteMapping("/book/{recycleIds}")
    public AjaxResult bookPurge(@PathVariable Long[] recycleIds)
    {
        return toAjax(recycleService.purgeBooks(recycleIds));
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:book:list')")
    @DeleteMapping("/book/clear")
    public AjaxResult bookClear()
    {
        return toAjax(recycleService.clearBooks());
    }

    // ================= 成员回收站 =================

    @PreAuthorize("@ss.hasPermi('system:recycle:reader:list')")
    @GetMapping("/reader/list")
    public TableDataInfo readerList(Reader query)
    {
        startPage();
        List<Reader> list = recycleService.listRecycleReaders(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:reader:list')")
    @GetMapping("/reader/count")
    public AjaxResult readerCount()
    {
        return success(recycleService.countReaders());
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:reader:list')")
    @PutMapping("/reader/restore/{recycleIds}")
    public AjaxResult readerRestore(@PathVariable Long[] recycleIds)
    {
        return toAjax(recycleService.restoreReaders(recycleIds));
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:reader:list')")
    @DeleteMapping("/reader/{recycleIds}")
    public AjaxResult readerPurge(@PathVariable Long[] recycleIds)
    {
        return toAjax(recycleService.purgeReaders(recycleIds));
    }

    @PreAuthorize("@ss.hasPermi('system:recycle:reader:list')")
    @DeleteMapping("/reader/clear")
    public AjaxResult readerClear()
    {
        return toAjax(recycleService.clearReaders());
    }
}