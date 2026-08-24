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
import com.ruoyi.common.utils.MailUtil;

/**
 * 服务候补Service业务层处理
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

    @Autowired
    private MailUtil mailUtil;

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

    /** 前台候补：校验成员/服务/库存/重复后创建（仅库存为0可候补，有库存提示直接借）
     * READ_COMMITTED：加锁后的重复候补/已借检查读最新已提交数据，并发重复候补才能被拦下 */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int reserveByCard(String cardNo, Long bookId)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || bookId == null)
        {
            throw new ServiceException("参数不完整");
        }
        // 先按证号定位成员，再锁成员行（FOR UPDATE）：同一成员的并发候补串行化，"重复候补"检查与插入原子化
        Reader queryReader = readerService.findActiveReader(cardNo);
        Reader reader = readerMapper.selectReaderByReaderIdForUpdate(queryReader.getReaderId());
        // findActiveReader 与加锁之间成员可能被管理端删除：加锁查不到即视为不存在
        if (reader == null)
        {
            throw new ServiceException("成员不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.READER_NORMAL.equals(reader.getStatus()))
        {
            throw new ServiceException("该成员编号已停用/挂失，无法候补");
        }
        // 锁服务行（FOR UPDATE，加锁顺序统一为 成员→服务，避免与报名路径交叉死锁）：
        // 与下架（changeBookStatus）/删除服务共享 book 行锁，下架完成后本事务读到的必是最新状态，
        // 下架与新建候补因此串行化，消除"下架校验通过后并发插入候补"的竞态（幽灵候补）
        Book book = bookMapper.selectBookByBookIdForUpdate(bookId);
        if (book == null || !"0".equals(book.getStatus()))
        {
            throw new ServiceException("该服务不存在或已下架");
        }
        if (book.getStock() != null && book.getStock() > 0)
        {
            throw new ServiceException("该服务当前有名额，可直接报名，无需候补");
        }
        // 已报名未完成不可候补（借走最后一本的人不能候补自己的书，完成后可直接再借）
        BorrowRecord bq = new BorrowRecord();
        bq.setReaderId(reader.getReaderId());
        bq.setBookId(bookId);
        List<BorrowRecord> borrowing = borrowRecordMapper.selectBorrowRecordList(bq);
        for (BorrowRecord br : borrowing)
        {
            if ("0".equals(br.getStatus()) || "2".equals(br.getStatus()))
            {
                throw new ServiceException("您已报名本服务且未完成，完成后可直接再报名，无需候补");
            }
        }
        // 重复候补校验（候补中/有名额状态下不可重复候补）
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
        int rows = bookReserveMapper.insertBookReserve(reserve);
        // 候补成功邮件（异步、尽力而为）
        mailUtil.sendHtml(reader.getEmail(), "【服务候补】候补成功",
                "<p>您好，" + esc(reader.getReaderName()) + "：</p>"
                + "<p>您已成功预约《" + esc(book.getBookName()) + "》。该书当前无库存，将进入预约队列；</p>"
                + "<p>一旦有名额释放，我们会通过邮件通知您。感谢支持数智游民创新工场！</p>");
        return rows;
    }

    /** HTML 转义 */
    private String esc(String s)
    {
        if (s == null) { return ""; }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public List<BookReserve> selectReservesByCard(String cardNo)
    {
        BookReserve query = new BookReserve();
        query.setCardNo(cardNo);
        return bookReserveMapper.selectBookReserveList(query);
    }

    /** 取消候补（仅候补中/有名额可取消） */
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
