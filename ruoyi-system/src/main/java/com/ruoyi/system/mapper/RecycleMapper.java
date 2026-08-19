package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;

/**
 * 回收站Mapper：删除时快照入库，支持还原（优先保原主键，被占用则自动换新）与彻底删除
 */
public interface RecycleMapper
{
    // ---------- 图书回收站 ----------
    List<Book> selectRecycleBookList(Book query);

    int insertRecycleBook(Book book);

    Book selectRecycleBookById(@Param("recycleId") Long recycleId);

    int deleteRecycleBookByIds(@Param("recycleIds") Long[] recycleIds);

    int clearRecycleBook();

    int countRecycleBook();

    // ---------- 读者回收站 ----------
    List<Reader> selectRecycleReaderList(Reader query);

    int insertRecycleReader(Reader reader);

    Reader selectRecycleReaderById(@Param("recycleId") Long recycleId);

    int deleteRecycleReaderByIds(@Param("recycleIds") Long[] recycleIds);

    int clearRecycleReader();

    int countRecycleReader();
}