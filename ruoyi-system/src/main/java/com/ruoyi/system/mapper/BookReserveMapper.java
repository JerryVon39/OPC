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

    /** 仅当预约仍为指定状态时流转（"可借"推进/超时取消用 CAS，防并发推进同一预约） */
    public int updateStatusIfCurrent(@org.apache.ibatis.annotations.Param("reserveId") Long reserveId,
            @org.apache.ibatis.annotations.Param("fromStatus") String fromStatus,
            @org.apache.ibatis.annotations.Param("toStatus") String toStatus,
            @org.apache.ibatis.annotations.Param("updateTime") java.util.Date updateTime);

    public int deleteBookReserveByReserveIds(Long[] reserveIds);
}
