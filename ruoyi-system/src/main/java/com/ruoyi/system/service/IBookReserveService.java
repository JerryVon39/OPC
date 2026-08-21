package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.BookReserve;

/**
 * 服务候补Service接口
 */
public interface IBookReserveService
{
    public BookReserve selectBookReserveByReserveId(Long reserveId);

    public List<BookReserve> selectBookReserveList(BookReserve bookReserve);

    public int insertBookReserve(BookReserve bookReserve);

    public int updateBookReserve(BookReserve bookReserve);

    public int deleteBookReserveByReserveIds(Long[] reserveIds);

    /** 前台候补：校验成员/服务/库存/重复后创建（库存为0才可候补） */
    public int reserveByCard(String cardNo, Long bookId);

    /** 我的候补（按证号） */
    public List<BookReserve> selectReservesByCard(String cardNo);

    /** 取消候补（仅候补中/有名额可取消） */
    public int cancelByCard(String cardNo, Long reserveId);
}
