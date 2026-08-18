package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.service.IBorrowRecordService;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.FineService;
import com.ruoyi.system.service.BorrowRuleService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.StatisticsService;

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

    @Autowired
    private com.ruoyi.system.mapper.SysNoticeMapper noticeMapper;

    @Autowired
    private com.ruoyi.system.mapper.BookReserveMapper bookReserveMapper;

    @Autowired
    private FineService fineService;

    @Autowired
    private BorrowRuleService borrowRuleService;

    @Autowired
    private IReaderService readerService;

    @Autowired
    private StatisticsService statisticsService;

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

    /** 借书前置校验：图书在架有库存 / 读者正常 / 无欠费 / 未重复借 / 未超上限 */
    private void validateBeforeBorrow(BorrowRecord borrowRecord, Book book, Reader reader)
    {
        if (book == null)
        {
            throw new ServiceException("图书不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.BOOK_ON_SALE.equals(book.getStatus()))
        {
            throw new ServiceException("该图书已下架，无法借出");
        }
        if (book.getStock() == null || book.getStock() <= 0)
        {
            throw new ServiceException("图书库存不足，无法借出");
        }
        if (reader == null)
        {
            throw new ServiceException("读者不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.READER_NORMAL.equals(reader.getStatus()))
        {
            throw new ServiceException("该读者证号已停用/挂失，无法借书");
        }
        fineService.checkNoUnpaidFine(borrowRecord.getReaderId());
        borrowRuleService.checkNotBorrowing(borrowRecord.getReaderId(), borrowRecord.getBookId());
        borrowRuleService.checkUnderLimit(borrowRecord.getReaderId(), borrowRuleService.maxCountFor(reader.getReaderType()));
    }

    /** 借书：创建记录 + 图书库存-1（事务：库存与记录同生共死）
     * READ_COMMITTED：FOR UPDATE 后的一致性读均读最新已提交数据，重复借阅/上限检查才能看到并发事务刚插入的记录 */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int insertBorrowRecord(BorrowRecord borrowRecord)
    {
        Book book = bookMapper.selectBookByBookId(borrowRecord.getBookId());
        // 锁读者行（FOR UPDATE）：同一读者的并发借书串行化，"重复借阅/借阅上限"检查与插入原子化
        Reader reader = readerMapper.selectReaderByReaderIdForUpdate(borrowRecord.getReaderId());
        // 前置校验（图书/读者/欠费/重复/上限）
        validateBeforeBorrow(borrowRecord, book, reader);
        String readerType = reader.getReaderType();
        // 借出日期默认今天，应还日期 = 借出 + 按类型的借期（学生/普通30天，教师60天，参数可配）
        if (borrowRecord.getBorrowDate() == null)
        {
            borrowRecord.setBorrowDate(new Date());
        }
        int days = borrowRuleService.daysFor(readerType);
        Date due = new Date(borrowRecord.getBorrowDate().getTime() + days * 24L * 3600 * 1000);
        borrowRecord.setDueDate(due);
        borrowRecord.setStatus(com.ruoyi.system.constant.BizStatus.BORROW_OUT);
        // 快照冗余：读者姓名/证号/书名写入借阅记录（删除读者/图书后历史记录仍完整，与订单快照语义一致）
        borrowRecord.setReaderName(reader.getReaderName());
        borrowRecord.setCardNo(reader.getCardNo());
        borrowRecord.setBookName(book.getBookName());
        // 库存 -1：原子条件更新（上方校验负责友好提示，此处兜底并发抢书）
        if (bookMapper.updateStock(book.getBookId(), 1L) == 0)
        {
            throw new ServiceException("图书库存不足，无法借出");
        }
        int rows = borrowRecordMapper.insertBorrowRecord(borrowRecord);
        // 借出成功后：该读者对该书的预约（预约中/可借）置为已完成
        com.ruoyi.system.domain.BookReserve rq = new com.ruoyi.system.domain.BookReserve();
        rq.setReaderId(borrowRecord.getReaderId());
        rq.setBookId(book.getBookId());
        java.util.List<com.ruoyi.system.domain.BookReserve> reserves = bookReserveMapper.selectBookReserveList(rq);
        for (com.ruoyi.system.domain.BookReserve rv : reserves)
        {
            if ("0".equals(rv.getStatus()) || "1".equals(rv.getStatus()))
            {
                rv.setStatus("2");
                rv.setUpdateTime(new Date());
                bookReserveMapper.updateBookReserve(rv);
            }
        }
        // 借阅数据变了：失效统计缓存（热门排行/看板下次请求重新计算）
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public int updateBorrowRecord(BorrowRecord borrowRecord)
    {
        // 生命周期字段守卫：状态/罚款/续借次数只能走还书/收款/续借专用流程，
        // 普通编辑不允许直接改（防绕过还书流程：借出中直标已归还而库存不还原）
        if (borrowRecord.getBorrowId() != null)
        {
            BorrowRecord old = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowRecord.getBorrowId());
            if (old != null)
            {
                if (borrowRecord.getStatus() != null && !borrowRecord.getStatus().equals(old.getStatus()))
                {
                    throw new ServiceException("借阅状态请通过还书/续借功能变更，不允许直接修改");
                }
                if (borrowRecord.getFineAmount() != null
                        && (old.getFineAmount() == null || borrowRecord.getFineAmount().compareTo(old.getFineAmount()) != 0))
                {
                    throw new ServiceException("罚款金额由系统在还书时自动结算，不允许直接修改");
                }
                if (borrowRecord.getFinePaid() != null && !borrowRecord.getFinePaid().equals(old.getFinePaid()))
                {
                    throw new ServiceException("罚款缴纳请使用收款功能，不允许直接修改");
                }
                if (borrowRecord.getRenewCount() != null
                        && (old.getRenewCount() == null || !borrowRecord.getRenewCount().equals(old.getRenewCount())))
                {
                    throw new ServiceException("续借次数由系统在续借时累加，不允许直接修改");
                }
            }
        }
        // 若读者/图书被修改，同步刷新快照（保持记录显示与实际一致）
        if (borrowRecord.getReaderId() != null)
        {
            Reader reader = readerMapper.selectReaderByReaderId(borrowRecord.getReaderId());
            if (reader != null)
            {
                borrowRecord.setReaderName(reader.getReaderName());
                borrowRecord.setCardNo(reader.getCardNo());
            }
        }
        if (borrowRecord.getBookId() != null)
        {
            Book book = bookMapper.selectBookByBookId(borrowRecord.getBookId());
            if (book != null)
            {
                borrowRecord.setBookName(book.getBookName());
            }
        }
        return borrowRecordMapper.updateBorrowRecord(borrowRecord);
    }

    /** 还书：置归还日期 + 状态已归还 + 图书库存+1（事务） */
    @Transactional
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
        // 先原子完成状态转换，只有抢到转换权的请求才能回补库存，避免并发双回补
        String fromStatus = record.getStatus();
        java.math.BigDecimal fine = fineService.calcFine(record);
        String finePaid = record.getFinePaid();
        if (fine != null)
        {
            finePaid = com.ruoyi.system.constant.BizStatus.FINE_UNPAID;
        }
        int rows = borrowRecordMapper.updateStatusIfCurrent(borrowId, fromStatus,
                com.ruoyi.system.constant.BizStatus.BORROW_RETURNED, new Date(), fine, finePaid, new Date());
        if (rows == 0)
        {
            throw new ServiceException("该图书已归还，请勿重复操作");
        }
        // 库存 +1（仅状态转换成功后回补）
        if (record.getBookId() != null)
        {
            bookMapper.restoreStock(record.getBookId(), 1L);
        }
        // 归还后同步逾期催还公告：还清了就删公告，还有逾期就重新汇总
        syncOverdueNotice();
        // 归还后检查预约：该书最早的"预约中"读者 → 状态置"可借"（前台我的预约可见）
        com.ruoyi.system.domain.BookReserve rq = new com.ruoyi.system.domain.BookReserve();
        rq.setBookId(record.getBookId());
        rq.setStatus("0");
        java.util.List<com.ruoyi.system.domain.BookReserve> reserves = bookReserveMapper.selectBookReserveList(rq);
        if (reserves != null && !reserves.isEmpty())
        {
            com.ruoyi.system.domain.BookReserve first = reserves.get(0);
            first.setStatus("1");
            first.setUpdateTime(new Date());
            bookReserveMapper.updateBookReserve(first);
        }
        // 借阅/罚款数据变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /** 罚款收款：标记已缴（收银台操作，委托 FineService） */
    @Override
    public int payFine(Long borrowId)
    {
        int rows = fineService.payFine(borrowId);
        // 未缴罚款总额变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 归还后同步逾期催还公告：
     * 1) 无剩余逾期记录 → 删除全部"逾期催还通知"公告
     * 2) 仍有逾期记录 → 删除旧公告并重新汇总发布（内容与现状一致）
     */
    private void syncOverdueNotice()
    {
        BorrowRecord q = new BorrowRecord();
        q.setStatus("2");
        List<BorrowRecord> overdue = borrowRecordMapper.selectBorrowRecordList(q);
        // 删除旧的催还公告
        com.ruoyi.system.domain.SysNotice delQuery = new com.ruoyi.system.domain.SysNotice();
        delQuery.setNoticeTitle("逾期催还通知");
        List<com.ruoyi.system.domain.SysNotice> oldList = noticeMapper.selectNoticeList(delQuery);
        for (com.ruoyi.system.domain.SysNotice n : oldList)
        {
            noticeMapper.deleteNoticeById(n.getNoticeId());
        }
        if (overdue == null || overdue.isEmpty())
        {
            return;
        }
        // 重新汇总发布（内容与当前逾期情况一致）
        String books = "";
        int max = Math.min(overdue.size(), 5);
        for (int i = 0; i < max; i++)
        {
            BorrowRecord br = overdue.get(i);
            books += "《" + (br.getBookName() == null ? "未知" : br.getBookName()) + "》(" + (br.getReaderName() == null ? "读者" : br.getReaderName()) + ") ";
        }
        if (overdue.size() > max)
        {
            books += "等共 " + overdue.size() + " 本";
        }
        com.ruoyi.system.domain.SysNotice notice = new com.ruoyi.system.domain.SysNotice();
        notice.setNoticeTitle("逾期催还通知");
        notice.setNoticeType("2");
        notice.setNoticeContent("以下图书已逾期未归还，请相关读者尽快到服务台办理还书：" + books);
        notice.setStatus("0");
        notice.setCreateBy("system");
        notice.setCreateTime(new Date());
        noticeMapper.insertNotice(notice);
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
        // 已逾期（真实日期判断）：逾期状态"2"是查询时动态算的、不落库，
        // 这里必须直接比较日期，否则逾期记录会漏过上面的 status 检查
        if (record.getDueDate().before(new Date()))
        {
            throw new ServiceException("该记录已逾期，请先归还后再借");
        }
        // 续借次数限制（委托 BorrowRuleService）
        long renewCount = borrowRuleService.checkRenewAllowed(record);
        // 应还日期 +30 天
        Date newDue = new Date(record.getDueDate().getTime() + 30L * 24 * 3600 * 1000);
        record.setDueDate(newDue);
        record.setRenewCount(renewCount + 1);
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
        Reader reader = readerService.findActiveReader(cardNo);
        BorrowRecord borrow = new BorrowRecord();
        borrow.setReaderId(reader.getReaderId());
        borrow.setBookId(bookId);
        return insertBorrowRecord(borrow);
    }

    /** 前台续借：证号归属校验 + 借出中 + 未逾期 → 应还日期 +30 天 */
    @Override
    @Transactional
    public int renewByCard(String cardNo, Long borrowId)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || borrowId == null)
        {
            throw new ServiceException("参数不完整");
        }
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
        if (record == null)
        {
            throw new ServiceException("借阅记录不存在");
        }
        // 证号归属校验：只能续借自己的书
        Reader reader = readerMapper.selectReaderByReaderId(record.getReaderId());
        if (reader == null || !cardNo.trim().equals(reader.getCardNo()))
        {
            throw new ServiceException("该借阅记录不属于此证号");
        }
        if ("1".equals(record.getStatus()))
        {
            throw new ServiceException("该图书已归还，无需续借");
        }
        if (record.getDueDate() == null)
        {
            throw new ServiceException("应还日期缺失，无法续借");
        }
        // 逾期判断（真实日期，逾期状态"2"不落库）
        if (record.getDueDate().before(new Date()))
        {
            throw new ServiceException("该记录已逾期，请先归还后再借");
        }
        // 续借次数限制（与后台 renewBook 一致，委托 BorrowRuleService）
        long renewCount = borrowRuleService.checkRenewAllowed(record);
        Date newDue = new Date(record.getDueDate().getTime() + 30L * 24 * 3600 * 1000);
        record.setDueDate(newDue);
        record.setRenewCount(renewCount + 1);
        record.setUpdateTime(new Date());
        return borrowRecordMapper.updateBorrowRecord(record);
    }

    /** 删除借阅记录：未还记录（借出中/逾期）先还原库存再删，有未缴罚款则拒绝（删除会抹掉欠费） */
    @Override
    @Transactional
    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds)
    {
        for (Long borrowId : borrowIds)
        {
            BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
            if (record == null)
            {
                continue;
            }
            if (com.ruoyi.system.constant.BizStatus.BORROW_OUT.equals(record.getStatus())
                    || com.ruoyi.system.constant.BizStatus.BORROW_OVERDUE.equals(record.getStatus()))
            {
                // 有未缴罚款的逾期记录不可删：先到服务台收款，否则欠费随记录一起消失
                if (record.getFineAmount() != null
                        && record.getFineAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                        && com.ruoyi.system.constant.BizStatus.FINE_UNPAID.equals(record.getFinePaid()))
                {
                    throw new ServiceException("该借阅记录有未缴罚款，请先到服务台收款后再删除");
                }
                // 未还书删除 = 库存还原（书回到书架，记录作废）
                if (record.getBookId() != null)
                {
                    bookMapper.restoreStock(record.getBookId(), 1L);
                }
            }
        }
        int rows = borrowRecordMapper.deleteBorrowRecordByBorrowIds(borrowIds);
        // 借阅数据变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public int deleteBorrowRecordByBorrowId(Long borrowId)
    {
        return borrowRecordMapper.deleteBorrowRecordByBorrowId(borrowId);
    }
}
