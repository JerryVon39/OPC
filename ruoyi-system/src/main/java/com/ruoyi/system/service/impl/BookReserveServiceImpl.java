package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BookReserve;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BookReserveMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.service.IBookReserveService;
import com.ruoyi.system.service.IReaderService;

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

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private IReaderService readerService;

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

    /** 前台预约：校验读者/图书/库存/重复后创建（仅库存为0可预约，有库存提示直接借）
     * READ_COMMITTED：加锁后的重复预约/已借检查读最新已提交数据，并发重复预约才能被拦下 */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int reserveByCard(String cardNo, Long bookId)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || bookId == null)
        {
            throw new ServiceException("参数不完整");
        }
        // 先按证号定位读者，再锁读者行（FOR UPDATE）：同一读者的并发预约串行化，"重复预约"检查与插入原子化
        Reader queryReader = readerService.findActiveReader(cardNo);
        Reader reader = readerMapper.selectReaderByReaderIdForUpdate(queryReader.getReaderId());
        // findActiveReader 与加锁之间读者可能被管理端删除：加锁查不到即视为不存在
        if (reader == null)
        {
            throw new ServiceException("读者不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.READER_NORMAL.equals(reader.getStatus()))
        {
            throw new ServiceException("该读者证号已停用/挂失，无法预约");
        }
        // 锁图书行（FOR UPDATE，加锁顺序统一为 读者→图书，避免与借书路径交叉死锁）：
        // 与下架（changeBookStatus）/删除图书共享 book 行锁，下架完成后本事务读到的必是最新状态，
        // 下架与新建预约因此串行化，消除"下架校验通过后并发插入预约"的竞态（幽灵预约）
        Book book = bookMapper.selectBookByBookIdForUpdate(bookId);
        if (book == null || !"0".equals(book.getStatus()))
        {
            throw new ServiceException("该图书不存在或已下架");
        }
        if (book.getStock() != null && book.getStock() > 0)
        {
            throw new ServiceException("该图书当前有库存，可直接借阅，无需预约");
        }
        // 已借阅未归还不可预约（借走最后一本的人不能预约自己的书，归还后可直接再借）
        BorrowRecord bq = new BorrowRecord();
        bq.setReaderId(reader.getReaderId());
        bq.setBookId(bookId);
        List<BorrowRecord> borrowing = borrowRecordMapper.selectBorrowRecordList(bq);
        for (BorrowRecord br : borrowing)
        {
            if ("0".equals(br.getStatus()) || "2".equals(br.getStatus()))
            {
                throw new ServiceException("您已借阅本书且未归还，归还后可直接再借，无需预约");
            }
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
        // 匿名接口空参守卫：null 直接 trim 会 NPE 变 500
        if (cardNo == null || cardNo.trim().isEmpty() || reserveId == null)
        {
            throw new ServiceException("参数不完整");
        }
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
