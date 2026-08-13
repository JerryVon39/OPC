package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.service.IBorrowRecordService;

/**
 * 借阅记录Service业务层处理
 * 包含借书/还书的业务规则（库存联动、逾期判断）
 */
@Service
public class BorrowRecordServiceImpl implements IBorrowRecordService
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @Override
    public BorrowRecord selectBorrowRecordByBorrowId(Long borrowId)
    {
        return borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
    }

    @Override
    public List<BorrowRecord> selectBorrowRecordList(BorrowRecord borrowRecord)
    {
        List<BorrowRecord> list = borrowRecordMapper.selectBorrowRecordList(borrowRecord);
        // 逾期动态判断：借出中(status=0)且应还日期已过 → 标记逾期(2)
        Date today = new Date();
        for (BorrowRecord br : list)
        {
            if ("0".equals(br.getStatus()) && br.getDueDate() != null && br.getDueDate().before(today))
            {
                br.setStatus("2");
            }
        }
        return list;
    }

    /** 借书：创建记录 + 图书库存-1 */
    @Override
    public int insertBorrowRecord(BorrowRecord borrowRecord)
    {
        Book book = bookMapper.selectBookByBookId(borrowRecord.getBookId());
        if (book == null)
        {
            throw new ServiceException("图书不存在");
        }
        if (!"0".equals(book.getStatus()))
        {
            throw new ServiceException("该图书已下架，无法借出");
        }
        if (book.getStock() == null || book.getStock() <= 0)
        {
            throw new ServiceException("图书库存不足，无法借出");
        }
        Reader reader = readerMapper.selectReaderByReaderId(borrowRecord.getReaderId());
        if (reader == null)
        {
            throw new ServiceException("读者不存在");
        }
        // 借出日期默认今天，应还日期 = 借出 + 30 天
        if (borrowRecord.getBorrowDate() == null)
        {
            borrowRecord.setBorrowDate(new Date());
        }
        Date due = new Date(borrowRecord.getBorrowDate().getTime() + 30L * 24 * 3600 * 1000);
        borrowRecord.setDueDate(due);
        borrowRecord.setStatus("0");
        // 库存 -1
        book.setStock(book.getStock() - 1);
        bookMapper.updateBook(book);
        return borrowRecordMapper.insertBorrowRecord(borrowRecord);
    }

    @Override
    public int updateBorrowRecord(BorrowRecord borrowRecord)
    {
        return borrowRecordMapper.updateBorrowRecord(borrowRecord);
    }

    /** 还书：置归还日期 + 状态已归还 + 图书库存+1 */
    public int returnBook(Long borrowId)
    {
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
        if (record == null)
        {
            throw new ServiceException("借阅记录不存在");
        }
        if ("1".equals(record.getStatus()))
        {
            throw new ServiceException("该图书已归还，请勿重复操作");
        }
        record.setReturnDate(new Date());
        record.setStatus("1");
        // 库存 +1
        Book book = bookMapper.selectBookByBookId(record.getBookId());
        if (book != null)
        {
            book.setStock((book.getStock() == null ? 0 : book.getStock()) + 1);
            bookMapper.updateBook(book);
        }
        return borrowRecordMapper.updateBorrowRecord(record);
    }

    @Override
    public List<BorrowRecord> selectBorrowListByCard(String cardNo)
    {
        List<BorrowRecord> list = borrowRecordMapper.selectBorrowListByCard(cardNo);
        Date today = new Date();
        for (BorrowRecord br : list)
        {
            if ("0".equals(br.getStatus()) && br.getDueDate() != null && br.getDueDate().before(today))
            {
                br.setStatus("2");
            }
        }
        return list;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> selectTopBooks()
    {
        return borrowRecordMapper.selectTopBooks(new BorrowRecord());
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> selectTopReaders()
    {
        return borrowRecordMapper.selectTopReaders(new BorrowRecord());
    }

    /** 续借：应还日期 +30 天 */
    @Override
    public int renewBook(Long borrowId)
    {
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
        if (record == null)
        {
            throw new ServiceException("借阅记录不存在");
        }
        if ("1".equals(record.getStatus()))
        {
            throw new ServiceException("该图书已归还，无需续借");
        }
        if ("2".equals(record.getStatus()))
        {
            throw new ServiceException("该记录已逾期，请先归还后再借");
        }
        if (record.getDueDate() == null)
        {
            throw new ServiceException("应还日期缺失，无法续借");
        }
        // 应还日期 +30 天
        Date newDue = new Date(record.getDueDate().getTime() + 30L * 24 * 3600 * 1000);
        record.setDueDate(newDue);
        record.setUpdateTime(new Date());
        return borrowRecordMapper.updateBorrowRecord(record);
    }

    /** 前台借书：按借书证号（匿名） */
    @Override
    public int borrowByCard(String cardNo, Long bookId)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            throw new ServiceException("请输入借书证号");
        }
        Reader query = new Reader();
        query.setCardNo(cardNo.trim());
        List<Reader> readers = readerMapper.selectReaderList(query);
        if (readers == null || readers.isEmpty())
        {
            throw new ServiceException("借书证号不存在，请先登记");
        }
        BorrowRecord borrow = new BorrowRecord();
        borrow.setReaderId(readers.get(0).getReaderId());
        borrow.setBookId(bookId);
        return insertBorrowRecord(borrow);
    }

    @Override
    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds)
    {
        return borrowRecordMapper.deleteBorrowRecordByBorrowIds(borrowIds);
    }

    @Override
    public int deleteBorrowRecordByBorrowId(Long borrowId)
    {
        return borrowRecordMapper.deleteBorrowRecordByBorrowId(borrowId);
    }
}
