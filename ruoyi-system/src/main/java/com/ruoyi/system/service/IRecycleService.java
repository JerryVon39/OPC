package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;

/**
 * 回收站服务：删除时快照入库（钩子调用）、列表、还原、彻底删除、清空
 */
public interface IRecycleService
{
    /** 删除图书前快照（deleteBookByBookIds 钩子调用） */
    void snapshotBook(Book book, String deletedBy);

    /** 删除读者前快照（deleteReaderByReaderIds 钩子调用） */
    void snapshotReader(Reader reader, String deletedBy);

    List<Book> listRecycleBooks(Book query);

    List<Reader> listRecycleReaders(Reader query);

    /** 还原图书：优先保原 book_id，被占用则自动分配新 id */
    int restoreBooks(Long[] recycleIds);

    /** 还原读者：优先保原 card_no，证号冲突则自动换新 */
    int restoreReaders(Long[] recycleIds);

    int purgeBooks(Long[] recycleIds);

    int purgeReaders(Long[] recycleIds);

    int clearBooks();

    int clearReaders();

    int countBooks();

    int countReaders();
}