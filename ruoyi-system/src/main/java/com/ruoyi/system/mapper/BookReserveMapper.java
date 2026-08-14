package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.BookReserve;

/**
 * 图书预约Mapper接口
 */
public interface BookReserveMapper
{
    public BookReserve selectBookReserveByReserveId(Long reserveId);

    public List<BookReserve> selectBookReserveList(BookReserve bookReserve);

    public int insertBookReserve(BookReserve bookReserve);

    public int updateBookReserve(BookReserve bookReserve);

    public int deleteBookReserveByReserveIds(Long[] reserveIds);
}
