package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.service.IBookService;
import com.ruoyi.system.service.StatisticsService;

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
    private StatisticsService statisticsService;

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
     * 批量删除图书信息
     * 
     * @param bookIds 需要删除的图书信息主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteBookByBookIds(Long[] bookIds)
    {
        for (Long bookId : bookIds)
        {
            // 锁图书行（FOR UPDATE）：防止检查通过后、删除前并发插入新的借阅/订单（检查与删除同事务原子化）
            if (bookMapper.selectBookByBookIdForUpdate(bookId) == null)
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
}
