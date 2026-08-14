package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.service.IBorrowRecordService;
import com.ruoyi.system.service.ISysConfigService;

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
    private ISysConfigService configService;

    @Autowired
    private com.ruoyi.system.mapper.SysNoticeMapper noticeMapper;

    @Autowired
    private com.ruoyi.system.mapper.BookReserveMapper bookReserveMapper;

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

    /** 借书：创建记录 + 图书库存-1（事务：库存与记录同生共死） */
    @Override
    @Transactional
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
        // 业务规则：证号状态校验（0正常可借，停用/挂失不可借）
        if (!"0".equals(reader.getStatus()))
        {
            throw new ServiceException("该读者证号已停用/挂失，无法借书");
        }
        // 业务规则：欠费冻结（有未缴罚款的读者不能借书）
        BorrowRecord fineQ = new BorrowRecord();
        fineQ.setReaderId(borrowRecord.getReaderId());
        fineQ.setFinePaid("0");
        List<BorrowRecord> unpaid = borrowRecordMapper.selectBorrowRecordList(fineQ);
        for (BorrowRecord fr : unpaid)
        {
            if (fr.getFineAmount() != null && fr.getFineAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
            {
                throw new ServiceException("该读者有未缴罚款（" + fr.getFineAmount() + " 元），请先到服务台缴费");
            }
        }
        // 业务规则：重复借阅校验（同一本书未还不可再借）
        java.util.List<BorrowRecord> exists = borrowRecordMapper.selectBorrowingByReaderAndBook(
                borrowRecord.getReaderId(), borrowRecord.getBookId());
        if (exists != null && !exists.isEmpty())
        {
            throw new ServiceException("该读者已借阅本书且未归还，请先还书");
        }
        // 业务规则：借阅数量上限（按读者类型区分：学生5/教师10/普通3，参数可配）
        String readerType = reader.getReaderType();
        int maxCount = configInt(typeKey("book.borrow.maxCount", readerType), 5);
        int borrowing = borrowRecordMapper.selectBorrowingCount(borrowRecord.getReaderId());
        if (borrowing >= maxCount)
        {
            throw new ServiceException("借阅数量已达上限（" + maxCount + " 本），请先归还部分图书");
        }
        // 借出日期默认今天，应还日期 = 借出 + 按类型的借期（学生/普通30天，教师60天，参数可配）
        if (borrowRecord.getBorrowDate() == null)
        {
            borrowRecord.setBorrowDate(new Date());
        }
        int days = configInt(typeKey("book.borrow.days", readerType), 30);
        Date due = new Date(borrowRecord.getBorrowDate().getTime() + days * 24L * 3600 * 1000);
        borrowRecord.setDueDate(due);
        borrowRecord.setStatus("0");
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
        return rows;
    }

    @Override
    public int updateBorrowRecord(BorrowRecord borrowRecord)
    {
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
        record.setReturnDate(new Date());
        record.setStatus("1");
        // 还书结算：逾期超过免罚天数，按天计罚（参数 book.fine.perDay / book.fine.graceDays）
        if (record.getDueDate() != null && record.getDueDate().before(new Date()))
        {
            int grace = configInt("book.fine.graceDays", 0);
            long overdueDays = (new Date().getTime() - record.getDueDate().getTime()) / (24L * 3600 * 1000);
            if (overdueDays > grace)
            {
                java.math.BigDecimal perDay = java.math.BigDecimal.valueOf(configDouble("book.fine.perDay", 0.1));
                java.math.BigDecimal fine = perDay.multiply(java.math.BigDecimal.valueOf(overdueDays - grace));
                record.setFineAmount(fine.setScale(2, java.math.RoundingMode.HALF_UP));
                record.setFinePaid("0");
            }
        }
        // 库存 +1（原子回补）
        if (record.getBookId() != null)
        {
            bookMapper.restoreStock(record.getBookId(), 1L);
        }
        int rows = borrowRecordMapper.updateBorrowRecord(record);
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
        return rows;
    }

    /** 罚款收款：标记已缴（收银台操作） */
    @Override
    public int payFine(Long borrowId)
    {
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
        if (record == null)
        {
            throw new ServiceException("借阅记录不存在");
        }
        if (record.getFineAmount() == null || record.getFineAmount().compareTo(java.math.BigDecimal.ZERO) <= 0
                || "1".equals(record.getFinePaid()))
        {
            throw new ServiceException("该记录无待缴罚款");
        }
        record.setFinePaid("1");
        record.setUpdateTime(new Date());
        return borrowRecordMapper.updateBorrowRecord(record);
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
        // 续借次数限制（参数 book.borrow.renewLimit，默认 1 次）
        long renewLimit = configInt("book.borrow.renewLimit", 1);
        long renewCount = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (renewCount >= renewLimit)
        {
            throw new ServiceException("该图书已续借过 " + renewCount + " 次，不可再次续借");
        }
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

    /** 按读者类型取参数键：book.borrow.maxCount.student（字典值1学生/2教师/3普通→语义后缀），无类型参数时回退通用键 */
    private String typeKey(String prefix, String readerType)
    {
        String suffix = "1".equals(readerType) ? "student"
                : "2".equals(readerType) ? "teacher"
                : "3".equals(readerType) ? "normal" : "";
        if (!suffix.isEmpty())
        {
            String key = prefix + "." + suffix;
            try
            {
                String v = configService.selectConfigByKey(key);
                if (v != null && !v.isEmpty())
                {
                    return key;
                }
            }
            catch (Exception ignore) { }
        }
        return prefix;
    }

    /** 读取参数整数，异常/空回退默认值 */
    private int configInt(String key, int def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            if (v != null && !v.isEmpty())
            {
                return Integer.parseInt(v);
            }
        }
        catch (Exception ignore) { }
        return def;
    }

    /** 读取参数小数（如罚款单价 0.10），异常/空回退默认值 */
    private double configDouble(String key, double def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            if (v != null && !v.isEmpty())
            {
                return Double.parseDouble(v);
            }
        }
        catch (Exception ignore) { }
        return def;
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
        // 续借次数限制（与后台 renewBook 一致）
        long renewLimit = configInt("book.borrow.renewLimit", 1);
        long renewCount = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (renewCount >= renewLimit)
        {
            throw new ServiceException("该图书已续借过 " + renewCount + " 次，不可再次续借");
        }
        Date newDue = new Date(record.getDueDate().getTime() + 30L * 24 * 3600 * 1000);
        record.setDueDate(newDue);
        record.setRenewCount(renewCount + 1);
        record.setUpdateTime(new Date());
        return borrowRecordMapper.updateBorrowRecord(record);
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
