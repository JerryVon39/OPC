package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.service.IBookService;

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
        return bookMapper.insertBook(book);
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
        return bookMapper.updateBook(book);
    }

    /**
     * 批量删除图书信息
     * 
     * @param bookIds 需要删除的图书信息主键
     * @return 结果
     */
    @Override
    public int deleteBookByBookIds(Long[] bookIds)
    {
        return bookMapper.deleteBookByBookIds(bookIds);
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
