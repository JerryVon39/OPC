package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.service.IBookService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 图书信息Controller
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
@RestController
@RequestMapping("/system/book")
public class BookController extends BaseController
{
    @Autowired
    private IBookService bookService;

    /**
     * 查询图书信息列表
     * @Anonymous 匿名访问：面向大众的公开接口（前台书店/小程序无需登录即可浏览）
     */
    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(Book book, HttpServletRequest request)
    {
        // 排序字段白名单：前台仅允许"借阅最多(borrowCount)/最新出版(publishDate)"等固定字段排序，防 ORDER BY 注入
        String orderByColumn = request.getParameter("orderByColumn");
        if (orderByColumn != null && !orderByColumn.trim().isEmpty())
        {
            String col = orderByColumn.trim();
            java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(
                    "book_id", "book_name", "author", "book_type", "publisher", "price", "publish_date", "stock", "status", "isbn",
                    "bookId", "bookName", "publishDate", "borrowCount", "borrow_count"));
            if (!allowed.contains(col))
            {
                throw new com.ruoyi.common.exception.ServiceException("非法的排序字段");
            }
        }
        // isAsc 也白名单化：只允许 asc/desc（及其长写法），防止经 isAsc 旁路在 ORDER BY 后追加任意列（防御纵深）
        String isAsc = request.getParameter("isAsc");
        if (isAsc != null && !isAsc.trim().isEmpty())
        {
            String dir = isAsc.trim();
            if (!"asc".equals(dir) && !"desc".equals(dir) && !"ascending".equals(dir) && !"descending".equals(dir))
            {
                throw new com.ruoyi.common.exception.ServiceException("非法的排序方向");
            }
        }
        startPage();
        List<Book> list = bookService.selectBookList(book);
        // 简介渲染 BBCODE（展示为富文本；后台编辑回显走 getInfo 不受影响）
        com.ruoyi.system.util.RenderUtil.renderBookIntro(list);
        return getDataTable(list);
    }

    /**
     * 同类图书推荐（匿名）：同分类在架书，排除自身
     */
    @Anonymous
    @GetMapping("/related")
    public AjaxResult related(Long bookId, String bookType)
    {
        if (bookId == null || bookType == null || bookType.trim().isEmpty())
        {
            return error("参数不完整");
        }
        java.util.List<Book> related = bookService.selectRelatedBooks(bookId, bookType.trim());
        com.ruoyi.system.util.RenderUtil.renderBookIntro(related);
        return success(related);
    }

    /**
     * 搜索联想（匿名）：输入时按书名模糊匹配在架图书，最多 8 条
     */
    @Anonymous
    @GetMapping("/suggest")
    public AjaxResult suggest(String keyword)
    {
        if (keyword == null || keyword.trim().isEmpty())
        {
            return success(java.util.Collections.emptyList());
        }
        return success(bookService.selectSuggestBooks(keyword.trim()));
    }

    /**
     * 导出图书信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:book:export')")
    @Log(title = "图书信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Book book)
    {
        List<Book> list = bookService.selectBookList(book);
        ExcelUtil<Book> util = new ExcelUtil<Book>(Book.class);
        util.exportExcel(response, list, "图书信息数据");
    }

    /**
     * 批量导入图书（Excel）：逐行校验，同名跳过，返回成功/失败明细
     */
    @PreAuthorize("@ss.hasPermi('system:book:add')")
    @Log(title = "图书信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(org.springframework.web.multipart.MultipartFile file) throws Exception
    {
        ExcelUtil<Book> util = new ExcelUtil<Book>(Book.class);
        List<Book> list = util.importExcel(file.getInputStream());
        return success(bookService.importBooks(list));
    }

    /**
     * 下载图书导入模板
     */
    @PreAuthorize("@ss.hasPermi('system:book:add')")
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Book> util = new ExcelUtil<Book>(Book.class);
        util.importTemplateExcel(response, "图书信息数据");
    }

    /**
     * 获取图书信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:book:query')")
    @GetMapping(value = "/{bookId}")
    public AjaxResult getInfo(@PathVariable("bookId") Long bookId)
    {
        return success(bookService.selectBookByBookId(bookId));
    }

    /**
     * 新增图书信息
     */
    @PreAuthorize("@ss.hasPermi('system:book:add')")
    @Log(title = "图书信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Book book)
    {
        return toAjax(bookService.insertBook(book));
    }

    /**
     * 修改图书信息
     */
    @PreAuthorize("@ss.hasPermi('system:book:edit')")
    @Log(title = "图书信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Book book)
    {
        return toAjax(bookService.updateBook(book));
    }

    /**
     * 上下架状态切换（后台列表开关）
     * 有预约中/可借预约的图书禁止下架（Service 层联动校验）
     */
    @PreAuthorize("@ss.hasPermi('system:book:edit')")
    @Log(title = "图书信息", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(Long bookId, String status)
    {
        return toAjax(bookService.changeBookStatus(bookId, status));
    }

    /**
     * 删除图书信息
     */
    @PreAuthorize("@ss.hasPermi('system:book:remove')")
    @Log(title = "图书信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{bookIds}")
    public AjaxResult remove(@PathVariable Long[] bookIds)
    {
        return toAjax(bookService.deleteBookByBookIds(bookIds));
    }
}
