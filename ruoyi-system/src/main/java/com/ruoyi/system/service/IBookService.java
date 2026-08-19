package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Book;

/**
 * 图书信息Service接口
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
public interface IBookService 
{
    /**
     * 查询图书信息
     * 
     * @param bookId 图书信息主键
     * @return 图书信息
     */
    public Book selectBookByBookId(Long bookId);

    /** 同类图书推荐：同分类在架书（最多4本） */
    public java.util.List<Book> selectRelatedBooks(Long bookId, String bookType);

    /** 搜索联想（匿名）：在架书按书名模糊匹配，最多 8 条 */
    public java.util.List<Book> selectSuggestBooks(String keyword);

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
     * 上下架状态切换（后台列表开关）
     * 有预约中/可借预约的图书禁止下架（联动校验）
     *
     * @param bookId 图书ID
     * @param status 目标状态（0在架 1下架）
     * @return 结果
     */
    public int changeBookStatus(Long bookId, String status);

    /**
     * 批量导入图书（Excel 逐行校验：必填/类型字典/同名判重跳过，错误收集行号明细）
     *
     * @param books 解析出的图书列表
     * @return {success: 成功条数, fail: 失败条数, errors: [行号+原因]}
     */
    public java.util.Map<String, Object> importBooks(java.util.List<Book> books);

    /**
     * 批量删除图书信息
     * 
     * @param bookIds 需要删除的图书信息主键集合
     * @return 结果
     */
    public int deleteBookByBookIds(Long[] bookIds);

    /**
     * 删除图书信息信息
     * 
     * @param bookId 图书信息主键
     * @return 结果
     */
    public int deleteBookByBookId(Long bookId);
}
