package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.BookReserve;

/**
 * 图书预约Service接口
 */
public interface IBookReserveService
{
    public BookReserve selectBookReserveByReserveId(Long reserveId);

    public List<BookReserve> selectBookReserveList(BookReserve bookReserve);

    public int insertBookReserve(BookReserve bookReserve);

    public int updateBookReserve(BookReserve bookReserve);

    public int deleteBookReserveByReserveIds(Long[] reserveIds);

    /** 前台预约：校验读者/图书/库存/重复后创建（库存为0才可预约） */
    public int reserveByCard(String cardNo, Long bookId);

    /** 我的预约（按证号） */
    public List<BookReserve> selectReservesByCard(String cardNo);

    /** 取消预约（仅预约中/可借可取消） */
    public int cancelByCard(String cardNo, Long reserveId);
}
