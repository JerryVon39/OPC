package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.BookReserve;

/**
 * 活动预约Mapper接口
 */
public interface BookReserveMapper
{
    public BookReserve selectBookReserveByReserveId(Long reserveId);

    public List<BookReserve> selectBookReserveList(BookReserve bookReserve);

    public int insertBookReserve(BookReserve bookReserve);

    public int updateBookReserve(BookReserve bookReserve);

    /** 补办换证号：同步该成员历史候补的快照证号（同一人换证号，"我的候补"仍可查全） */
    public int updateCardNoSnapshot(@org.apache.ibatis.annotations.Param("readerId") Long readerId,
            @org.apache.ibatis.annotations.Param("newCardNo") String newCardNo);

    /** 仅当预约仍为指定状态时流转（"可借"推进/超时取消用 CAS，防并发推进同一预约） */
    public int updateStatusIfCurrent(@org.apache.ibatis.annotations.Param("reserveId") Long reserveId,
            @org.apache.ibatis.annotations.Param("fromStatus") String fromStatus,
            @org.apache.ibatis.annotations.Param("toStatus") String toStatus,
            @org.apache.ibatis.annotations.Param("updateTime") java.util.Date updateTime);

    public int deleteBookReserveByReserveIds(Long[] reserveIds);
}
