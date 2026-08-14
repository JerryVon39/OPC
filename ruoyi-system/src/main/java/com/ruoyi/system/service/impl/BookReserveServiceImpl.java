package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BookReserve;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BookReserveMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.service.IBookReserveService;

/**
 * 图书预约Service业务层处理
 */
@Service
public class BookReserveServiceImpl implements IBookReserveService
{
    @Autowired
    private BookReserveMapper bookReserveMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @Override
    public BookReserve selectBookReserveByReserveId(Long reserveId)
    {
        return bookReserveMapper.selectBookReserveByReserveId(reserveId);
    }

    @Override
    public List<BookReserve> selectBookReserveList(BookReserve bookReserve)
    {
        return bookReserveMapper.selectBookReserveList(bookReserve);
    }

    @Override
    public int insertBookReserve(BookReserve bookReserve)
    {
        return bookReserveMapper.insertBookReserve(bookReserve);
    }

    @Override
    public int updateBookReserve(BookReserve bookReserve)
    {
        return bookReserveMapper.updateBookReserve(bookReserve);
    }

    @Override
    public int deleteBookReserveByReserveIds(Long[] reserveIds)
    {
        return bookReserveMapper.deleteBookReserveByReserveIds(reserveIds);
    }

    /** 前台预约：校验读者/图书/库存/重复后创建（仅库存为0可预约，有库存提示直接借） */
    @Override
    @Transactional
    public int reserveByCard(String cardNo, Long bookId)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || bookId == null)
        {
            throw new ServiceException("参数不完整");
        }
        Reader query = new Reader();
        query.setCardNo(cardNo.trim());
        List<Reader> readers = readerMapper.selectReaderList(query);
        if (readers == null || readers.isEmpty())
        {
            throw new ServiceException("借书证号不存在，请先登记");
        }
        Reader reader = readers.get(0);
        if (!"0".equals(reader.getStatus()))
        {
            throw new ServiceException("该读者证号已停用/挂失，无法预约");
        }
        Book book = bookMapper.selectBookByBookId(bookId);
        if (book == null || !"0".equals(book.getStatus()))
        {
            throw new ServiceException("该图书不存在或已下架");
        }
        if (book.getStock() != null && book.getStock() > 0)
        {
            throw new ServiceException("该图书当前有库存，可直接借阅，无需预约");
        }
        // 重复预约校验（预约中/可借状态下不可重复预约）
        BookReserve q = new BookReserve();
        q.setCardNo(cardNo.trim());
        q.setBookId(bookId);
        List<BookReserve> exists = bookReserveMapper.selectBookReserveList(q);
        for (BookReserve r : exists)
        {
            if ("0".equals(r.getStatus()) || "1".equals(r.getStatus()))
            {
                throw new ServiceException("您已预约该书，请勿重复预约");
            }
        }
        BookReserve reserve = new BookReserve();
        reserve.setBookId(book.getBookId());
        reserve.setReaderId(reader.getReaderId());
        reserve.setReaderName(reader.getReaderName());
        reserve.setCardNo(reader.getCardNo());
        reserve.setBookName(book.getBookName());
        reserve.setReserveDate(new Date());
        reserve.setStatus("0");
        reserve.setCreateBy(reader.getReaderName());
        reserve.setCreateTime(new Date());
        return bookReserveMapper.insertBookReserve(reserve);
    }

    @Override
    public List<BookReserve> selectReservesByCard(String cardNo)
    {
        BookReserve query = new BookReserve();
        query.setCardNo(cardNo);
        return bookReserveMapper.selectBookReserveList(query);
    }

    /** 取消预约（仅预约中/可借可取消） */
    @Override
    public int cancelByCard(String cardNo, Long reserveId)
    {
        BookReserve reserve = bookReserveMapper.selectBookReserveByReserveId(reserveId);
        if (reserve == null)
        {
            throw new ServiceException("预约记录不存在");
        }
        if (!cardNo.trim().equals(reserve.getCardNo()))
        {
            throw new ServiceException("该预约不属于此证号");
        }
        if (!"0".equals(reserve.getStatus()) && !"1".equals(reserve.getStatus()))
        {
            throw new ServiceException("该预约已结束，无法取消");
        }
        reserve.setStatus("3");
        reserve.setUpdateTime(new Date());
        return bookReserveMapper.updateBookReserve(reserve);
    }
}
