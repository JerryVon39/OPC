package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.RecycleMapper;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.IRecycleService;

/**
 * 回收站服务实现：删除钩子快照 + 还原（优先保原主键/证号，冲突自动换新）+ 彻底删除/清空
 */
@Service
public class RecycleServiceImpl implements IRecycleService
{
    @Autowired
    private RecycleMapper recycleMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @Autowired
    @Lazy
    private IReaderService readerService;

    /** 取当前操作人（未登录/定时任务场景返回空串，不抛异常） */
    private String operator()
    {
        try { return SecurityUtils.getUsername(); }
        catch (Exception e) { return ""; }
    }

    @Override
    public void snapshotBook(Book book, String deletedBy)
    {
        book.setDeletedBy(deletedBy == null ? operator() : deletedBy);
        book.setDeletedTime(new Date());
        recycleMapper.insertRecycleBook(book);
    }

    @Override
    public void snapshotReader(Reader reader, String deletedBy)
    {
        reader.setDeletedBy(deletedBy == null ? operator() : deletedBy);
        reader.setDeletedTime(new Date());
        recycleMapper.insertRecycleReader(reader);
    }

    @Override
    public List<Book> listRecycleBooks(Book query)
    {
        return recycleMapper.selectRecycleBookList(query);
    }

    @Override
    public List<Reader> listRecycleReaders(Reader query)
    {
        return recycleMapper.selectRecycleReaderList(query);
    }

    /**
     * 还原服务：优先保原 book_id（历史关联更强）；若期间该 id 已被新书占用，自动分配新 id
     */
    @Override
    @Transactional
    public int restoreBooks(Long[] recycleIds)
    {
        int ok = 0;
        for (Long rid : recycleIds)
        {
            Book rc = recycleMapper.selectRecycleBookById(rid);
            if (rc == null) { continue; }
            rc.setRecycleId(null);
            rc.setDeletedBy(null);
            rc.setDeletedTime(null);
            // 原 id 空闲则保留，否则置空走自增新 id
            if (rc.getBookId() != null && bookMapper.selectBookByBookId(rc.getBookId()) != null)
            {
                rc.setBookId(null);
            }
            if (bookMapper.insertBook(rc) > 0)
            {
                recycleMapper.deleteRecycleBookByIds(new Long[] { rid });
                ok++;
            }
        }
        return ok;
    }

    /**
     * 还原成员：优先保原 card_no（历史快照按证号关联）；若期间证号已被占用，自动换新证号
     */
    @Override
    @Transactional
    public int restoreReaders(Long[] recycleIds)
    {
        int ok = 0;
        for (Long rid : recycleIds)
        {
            Reader rc = recycleMapper.selectRecycleReaderById(rid);
            if (rc == null) { continue; }
            rc.setRecycleId(null);
            rc.setDeletedBy(null);
            rc.setDeletedTime(null);
            // 证号冲突则置空，由 ReaderServiceImpl.insertReader 自动生成新证号
            if (rc.getCardNo() != null && !rc.getCardNo().isEmpty()
                    && readerMapper.countByCardNo(rc.getCardNo()) > 0)
            {
                rc.setCardNo(null);
            }
            if (readerService.insertReader(rc) > 0)
            {
                recycleMapper.deleteRecycleReaderByIds(new Long[] { rid });
                ok++;
            }
        }
        return ok;
    }

    @Override
    public int purgeBooks(Long[] recycleIds)
    {
        return recycleMapper.deleteRecycleBookByIds(recycleIds);
    }

    @Override
    public int purgeReaders(Long[] recycleIds)
    {
        return recycleMapper.deleteRecycleReaderByIds(recycleIds);
    }

    @Override
    public int clearBooks() { return recycleMapper.clearRecycleBook(); }

    @Override
    public int clearReaders() { return recycleMapper.clearRecycleReader(); }

    @Override
    public int countBooks() { return recycleMapper.countRecycleBook(); }

    @Override
    public int countReaders() { return recycleMapper.countRecycleReader(); }
}