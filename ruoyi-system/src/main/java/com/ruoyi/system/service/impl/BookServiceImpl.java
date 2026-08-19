package com.ruoyi.system.service.impl;

import java.util.Arrays;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BookReserve;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BookReserveMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.service.IBookService;
import com.ruoyi.system.service.IRecycleService;
import com.ruoyi.system.service.StatisticsService;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.service.ISysDictDataService;

/**
 * 图书信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
@Service
public class BookServiceImpl implements IBookService 
{
    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private ShopOrderMapper shopOrderMapper;

    @Autowired
    private BookReserveMapper bookReserveMapper;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private IRecycleService recycleService;

    /**
     * 查询图书信息
     * 
     * @param bookId 图书信息主键
     * @return 图书信息
     */
    @Override
    public Book selectBookByBookId(Long bookId)
    {
        return bookMapper.selectBookByBookId(bookId);
    }

    /** 同类图书推荐：同分类在架书（最多4本） */
    @Override
    public java.util.List<Book> selectRelatedBooks(Long bookId, String bookType)
    {
        return bookMapper.selectRelatedBooks(bookId, bookType);
    }

    /** 搜索联想（匿名）：在架书按书名模糊匹配，最多 8 条 */
    @Override
    public java.util.List<Book> selectSuggestBooks(String keyword)
    {
        return bookMapper.selectSuggestBooks(keyword);
    }

    /**
     * 查询图书信息列表
     * 
     * @param book 图书信息
     * @return 图书信息
     */
    @Override
    public List<Book> selectBookList(Book book)
    {
        return bookMapper.selectBookList(book);
    }

    /**
     * 新增图书信息
     * 
     * @param book 图书信息
     * @return 结果
     */
    @Override
    public int insertBook(Book book)
    {
        book.setCreateTime(DateUtils.getNowDate());
        int rows = bookMapper.insertBook(book);
        // 馆藏总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 修改图书信息
     * 
     * @param book 图书信息
     * @return 结果
     */
    @Override
    public int updateBook(Book book)
    {
        book.setUpdateTime(DateUtils.getNowDate());
        int rows = bookMapper.updateBook(book);
        // 上架/下架影响在架统计：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 上下架状态切换（后台列表开关）
     * 有预约中/可借预约的图书禁止下架（联动校验）
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int changeBookStatus(Long bookId, String status)
    {
        if (bookId == null)
        {
            throw new com.ruoyi.common.exception.ServiceException("参数不完整");
        }
        if (!"0".equals(status) && !"1".equals(status))
        {
            throw new com.ruoyi.common.exception.ServiceException("非法的上下架状态");
        }
        // 锁图书行：防止检查通过后并发新增预约再下架（检查与更新同事务原子化）
        Book book = bookMapper.selectBookByBookIdForUpdate(bookId);
        if (book == null)
        {
            throw new com.ruoyi.common.exception.ServiceException("图书不存在");
        }
        // 仅"在架 → 下架"需要联动校验；上架永远允许
        boolean goingOff = "1".equals(status) && "0".equals(book.getStatus());
        if (goingOff)
        {
            BookReserve rq = new BookReserve();
            rq.setBookId(bookId);
            List<BookReserve> reserves = bookReserveMapper.selectBookReserveList(rq);
            for (BookReserve rv : reserves)
            {
                if ("0".equals(rv.getStatus()) || "1".equals(rv.getStatus()))
                {
                    throw new com.ruoyi.common.exception.ServiceException("该图书存在预约中的读者，请先取消预约后再下架");
                }
            }
        }
        int rows = bookMapper.updateBookStatus(bookId, status);
        if (rows > 0)
        {
            // 上架/下架影响在架统计：失效统计缓存
            statisticsService.evictAll();
        }
        return rows;
    }

    /**
     * 批量删除图书信息
     * 
     * @param bookIds 需要删除的图书信息主键
     * @return 结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int deleteBookByBookIds(Long[] bookIds)
    {
        // 排序：批量删除的加锁顺序一致，避免并发批量删除互相持锁等待（死锁）
        Arrays.sort(bookIds);
        java.util.List<Book> toSnapshot = new java.util.ArrayList<>();
        for (Long bookId : bookIds)
        {
            // 锁图书行（FOR UPDATE）：防止检查通过后、删除前并发插入新的借阅/订单（检查与删除同事务原子化）
            Book book = bookMapper.selectBookByBookIdForUpdate(bookId);
            if (book == null)
            {
                continue;
            }
            // 有未归还借阅（借出中/逾期）的图书不可删
            BorrowRecord q = new BorrowRecord();
            q.setBookId(bookId);
            List<BorrowRecord> records = borrowRecordMapper.selectBorrowRecordList(q);
            for (BorrowRecord r : records)
            {
                if ("0".equals(r.getStatus()) || "2".equals(r.getStatus()))
                {
                    throw new com.ruoyi.common.exception.ServiceException("《" + (r.getBookName() == null ? "该图书" : r.getBookName()) + "》存在未归还的借阅记录，无法删除");
                }
            }
            // 有待处理订单的图书不可删
            ShopOrder oq = new ShopOrder();
            oq.setBookId(bookId);
            oq.setStatus("0");
            List<ShopOrder> orders = shopOrderMapper.selectShopOrderList(oq);
            if (orders != null && !orders.isEmpty())
            {
                throw new com.ruoyi.common.exception.ServiceException("该图书存在待处理订单，无法删除");
            }
            // 有预约中/可借预约的图书不可删（否则前台"我的预约"出现幽灵记录）
            BookReserve rq = new BookReserve();
            rq.setBookId(bookId);
            List<BookReserve> reserves = bookReserveMapper.selectBookReserveList(rq);
            for (BookReserve rv : reserves)
            {
                if ("0".equals(rv.getStatus()) || "1".equals(rv.getStatus()))
                {
                    throw new com.ruoyi.common.exception.ServiceException("该图书存在预约中的读者，请先取消预约后再删除");
                }
            }
            // 校验全部通过：记入待快照集合，供误删后回收站还原
            toSnapshot.add(book);
        }
        // 物理删除前先把通过校验的图书快照进回收站（同事务，任一失败整体回滚）
        for (Book b : toSnapshot)
        {
            recycleService.snapshotBook(b, null);
        }
        int rows = bookMapper.deleteBookByBookIds(bookIds);
        // 馆藏总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 删除图书信息信息
     *
     * @param bookId 图书信息主键
     * @return 结果
     */
    @Override
    public int deleteBookByBookId(Long bookId)
    {
        return bookMapper.deleteBookByBookId(bookId);
    }

    /**
     * 批量导入图书：逐行校验（书名必填/类型字典/同名判重跳过），
     * 错误不中断整批，收集行号明细返回前端展示
     */
    @Override
    public java.util.Map<String, Object> importBooks(java.util.List<Book> books)
    {
        // 类型字典值集合（一次查询复用整批）
        java.util.Set<String> typeSet = new java.util.HashSet<>();
        SysDictData typeQuery = new SysDictData();
        typeQuery.setDictType("book_type");
        for (SysDictData d : sysDictDataService.selectDictDataList(typeQuery))
        {
            typeSet.add(d.getDictValue());
        }
        int success = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        for (int i = 0; i < books.size(); i++)
        {
            Book b = books.get(i);
            if (b == null)
            {
                continue; // Excel 尾部空行解析为 null，跳过
            }
            int row = i + 2; // 模板第 1 行为大标题、第 2 行为列名，数据从第 3 行起
            if (b.getBookName() == null || b.getBookName().trim().isEmpty())
            {
                errors.add("第" + row + "行：书名不能为空");
                continue;
            }
            b.setBookName(b.getBookName().trim());
            if (b.getBookType() != null && !b.getBookType().trim().isEmpty() && !typeSet.contains(b.getBookType().trim()))
            {
                errors.add("第" + row + "行：《" + b.getBookName() + "》图书类型不在字典内");
                continue;
            }
            if (bookMapper.countByBookName(b.getBookName()) > 0)
            {
                errors.add("第" + row + "行：《" + b.getBookName() + "》已存在，已跳过");
                continue;
            }
            b.setStatus(b.getStatus() == null || b.getStatus().trim().isEmpty() ? "0" : b.getStatus());
            insertBook(b);
            success++;
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", success);
        result.put("fail", errors.size());
        result.put("errors", errors);
        return result;
    }
}
