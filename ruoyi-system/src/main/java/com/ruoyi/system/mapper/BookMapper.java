package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Book;

/**
 * 服务信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
public interface BookMapper 
{
    /**
     * 查询服务信息
     * 
     * @param bookId 服务信息主键
     * @return 服务信息
     */
    public Book selectBookByBookId(Long bookId);

    /** 行锁查询（FOR UPDATE）：删除服务时锁行，防止检查后插入新报名/订单的竞态 */
    public Book selectBookByBookIdForUpdate(Long bookId);

    /**
     * 查询服务信息列表
     * 
     * @param book 服务信息
     * @return 服务信息集合
     */
    public List<Book> selectBookList(Book book);

    /**
     * 新增服务信息
     * 
     * @param book 服务信息
     * @return 结果
     */
    public int insertBook(Book book);

    /**
     * 修改服务信息
     * 
     * @param book 服务信息
     * @return 结果
     */
    public int updateBook(Book book);

    /**
     * 删除服务信息
     * 
     * @param bookId 服务信息主键
     * @return 结果
     */
    public int deleteBookByBookId(Long bookId);

    /**
     * 批量删除服务信息
     * 
     * @param bookIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBookByBookIds(Long[] bookIds);

    /** 原子扣减库存：仅当库存充足才扣（并发下不超卖），返回影响行数（0=库存不足） */
    public int updateStock(@Param("bookId") Long bookId, @Param("quantity") Long quantity);

    /** 回补库存（完成/取消订单） */
    public int restoreStock(@Param("bookId") Long bookId, @Param("quantity") Long quantity);

    /** 上下架状态切换（后台列表开关）：仅更新 status，不触碰其他字段 */
    public int updateBookStatus(@Param("bookId") Long bookId, @Param("status") String status);

    /** 同类服务推荐：同分类在架书（排除自身，最多4本） */
    public java.util.List<Book> selectRelatedBooks(@Param("bookId") Long bookId, @Param("bookType") String bookType);

    /** 搜索联想（匿名）：书架书按书名模糊匹配，最多 8 条（返回 bookId/bookName/author 等轻量字段） */
    public java.util.List<Book> selectSuggestBooks(@Param("keyword") String keyword);

    /** 库存预警：在架书且库存 <= 阈值，按库存升序取前 N 条（后台看板提醒补货） */
    public java.util.List<Book> selectLowStockBooks(@Param("threshold") int threshold, @Param("limit") int limit);

    /** 批量导入判重：同名服务是否存在 */
    public int countByBookName(@Param("bookName") String bookName);
}
