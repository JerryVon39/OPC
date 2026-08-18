package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Book;

/**
 * 图书信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
public interface BookMapper 
{
    /**
     * 查询图书信息
     * 
     * @param bookId 图书信息主键
     * @return 图书信息
     */
    public Book selectBookByBookId(Long bookId);

    /** 行锁查询（FOR UPDATE）：删除图书时锁行，防止检查后插入新借阅/订单的竞态 */
    public Book selectBookByBookIdForUpdate(Long bookId);

    /**
     * 查询图书信息列表
     * 
     * @param book 图书信息
     * @return 图书信息集合
     */
    public List<Book> selectBookList(Book book);

    /**
     * 新增图书信息
     * 
     * @param book 图书信息
     * @return 结果
     */
    public int insertBook(Book book);

    /**
     * 修改图书信息
     * 
     * @param book 图书信息
     * @return 结果
     */
    public int updateBook(Book book);

    /**
     * 删除图书信息
     * 
     * @param bookId 图书信息主键
     * @return 结果
     */
    public int deleteBookByBookId(Long bookId);

    /**
     * 批量删除图书信息
     * 
     * @param bookIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBookByBookIds(Long[] bookIds);

    /** 原子扣减库存：仅当库存充足才扣（并发下不超卖），返回影响行数（0=库存不足） */
    public int updateStock(@Param("bookId") Long bookId, @Param("quantity") Long quantity);

    /** 回补库存（归还/取消订单） */
    public int restoreStock(@Param("bookId") Long bookId, @Param("quantity") Long quantity);

    /** 同类图书推荐：同分类在架书（排除自身，最多4本） */
    public java.util.List<Book> selectRelatedBooks(@Param("bookId") Long bookId, @Param("bookType") String bookType);
}
