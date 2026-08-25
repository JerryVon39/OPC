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
import com.ruoyi.system.service.IMailTemplateService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.StatisticsService;

/**
 * 报名记录Service业务层处理
 * 包含报名/完成的业务规则（库存联动、逾期判断）
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

    @Autowired
    private IMailTemplateService mailTemplateService;

    @Autowired
    private ISysConfigService configService;

    @Override
    public BorrowRecord selectBorrowRecordByBorrowId(Long borrowId)
    {
        return borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
    }

    @Override
    public List<BorrowRecord> selectBorrowRecordList(BorrowRecord borrowRecord)
    {
        List<BorrowRecord> list = borrowRecordMapper.selectBorrowRecordList(borrowRecord);
        // 逾期动态判断：进行中(status=0)且截止日期已过 → 标记逾期(2)
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

    /** 报名前置校验：服务在架有库存 / 成员正常 / 无欠费 / 未重复借 / 未超上限 */
    private void validateBeforeBorrow(BorrowRecord borrowRecord, Book book, Reader reader)
    {
        if (book == null)
        {
            throw new ServiceException("服务不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.BOOK_ON_SALE.equals(book.getStatus()))
        {
            throw new ServiceException("该服务已下架，无法报名");
        }
        if (book.getStock() == null || book.getStock() <= 0)
        {
            throw new ServiceException("服务名额不足，无法报名");
        }
        if (reader == null)
        {
            throw new ServiceException("成员不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.READER_NORMAL.equals(reader.getStatus()))
        {
            throw new ServiceException("该成员编号已停用，无法报名");
        }
        fineService.checkNoUnpaidFine(borrowRecord.getReaderId());
        borrowRuleService.checkNotBorrowing(borrowRecord.getReaderId(), borrowRecord.getBookId());
        borrowRuleService.checkUnderLimit(borrowRecord.getReaderId(), borrowRuleService.maxCountFor(reader.getReaderType()));
    }

    /** 报名：创建记录 + 服务库存-1（事务：库存与记录同生共死）
     * READ_COMMITTED：FOR UPDATE 后的一致性读均读最新已提交数据，重复报名/上限检查才能看到并发事务刚插入的记录 */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int insertBorrowRecord(BorrowRecord borrowRecord)
    {
        // 锁成员行（FOR UPDATE）：同一成员的并发报名串行化，"重复报名/报名上限"检查与插入原子化
        Reader reader = readerMapper.selectReaderByReaderIdForUpdate(borrowRecord.getReaderId());
        // 再锁服务行（FOR UPDATE，加锁顺序统一为 成员→服务，避免与候补路径交叉死锁）：
        // 与下架（changeBookStatus）/删除服务共享 book 行锁，下架完成后本事务读到的必是最新状态，
        // 已下架的书在此被拦截（"该服务已下架，无法报名"），下架与新报名因此串行化
        Book book = bookMapper.selectBookByBookIdForUpdate(borrowRecord.getBookId());
        // 前置校验（服务/成员/欠费/重复/上限）
        validateBeforeBorrow(borrowRecord, book, reader);
        // 该书存在"有名额"候补（完成后已通知取书）时仅候补人有名额：
        // 防止候补成员赶来时书已被他人借走（候补白等、队列失真）。服务行锁已持有，检查与插入原子化
        com.ruoyi.system.domain.BookReserve prq = new com.ruoyi.system.domain.BookReserve();
        prq.setBookId(book.getBookId());
        prq.setStatus("1");
        java.util.List<com.ruoyi.system.domain.BookReserve> pickups = bookReserveMapper.selectBookReserveList(prq);
        if (pickups != null && !pickups.isEmpty())
        {
            boolean isPickupReader = false;
            for (com.ruoyi.system.domain.BookReserve p : pickups)
            {
                if (p.getReaderId() != null && p.getReaderId().equals(borrowRecord.getReaderId()))
                {
                    isPickupReader = true;
                    break;
                }
            }
            if (!isPickupReader)
            {
                throw new ServiceException("该服务已有候补成员获得名额，请候补成员优先报名");
            }
        }
        String readerType = reader.getReaderType();
        // 报名日期默认今天，截止日期 = 报名 + 按类型的借期（学生/普通30天，教师60天，参数可配）
        if (borrowRecord.getBorrowDate() == null)
        {
            borrowRecord.setBorrowDate(new Date());
        }
        int days = borrowRuleService.daysFor(readerType);
        Date due = new Date(borrowRecord.getBorrowDate().getTime() + days * 24L * 3600 * 1000);
        borrowRecord.setDueDate(due);
        borrowRecord.setStatus(com.ruoyi.system.constant.BizStatus.BORROW_OUT);
        // 快照冗余：成员姓名/证号/书名写入报名记录（删除成员/服务后历史记录仍完整，与订单快照语义一致）
        borrowRecord.setReaderName(reader.getReaderName());
        borrowRecord.setCardNo(reader.getCardNo());
        borrowRecord.setBookName(book.getBookName());
        // 库存 -1：原子条件更新（上方校验负责友好提示，此处兜底并发抢书）
        if (bookMapper.updateStock(book.getBookId(), 1L) == 0)
        {
            throw new ServiceException("服务名额不足，无法报名");
        }
        int rows = borrowRecordMapper.insertBorrowRecord(borrowRecord);
        // 报名成功后：该成员对该书的候补（候补中/有名额）置为已完成
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
        // 报名数据变了：失效统计缓存（热门排行/看板下次请求重新计算）
        statisticsService.evictAll();
        // 报名成功邮件通知（模板渲染、异步、尽力而为，失败不影响业务）
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("readerName", reader.getReaderName());
        params.put("bookName", book.getBookName());
        params.put("dueDate", com.ruoyi.common.utils.DateUtils.parseDateToStr("yyyy-MM-dd", due));
        mailTemplateService.send("borrow.success", reader.getEmail(), params);
        return rows;
    }

    @Override
    public int updateBorrowRecord(BorrowRecord borrowRecord)
    {
        // 生命周期字段守卫：状态/罚款/续期次数只能走完成/收款/续期专用流程，
        // 普通编辑不允许直接改（防绕过完成流程：进行中直标已完成而库存不还原）
        if (borrowRecord.getBorrowId() != null)
        {
            BorrowRecord old = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowRecord.getBorrowId());
            if (old != null)
            {
                // 动态逾期例外：列表/详情回显的"2(逾期)"是按日期实时算的、不落库（库中仍是"0"），
                // 编辑备注等字段会把回显的"2"带回来，与库中"0"的差异不算生命周期变更，放行
                String newStatus = borrowRecord.getStatus();
                boolean dynamicOverdue = "2".equals(newStatus) && "0".equals(old.getStatus());
                if (newStatus != null && !dynamicOverdue && !newStatus.equals(old.getStatus()))
                {
                    throw new ServiceException("报名状态请通过完成/续期功能变更，不允许直接修改");
                }
                if (borrowRecord.getFineAmount() != null
                        && (old.getFineAmount() == null || borrowRecord.getFineAmount().compareTo(old.getFineAmount()) != 0))
                {
                    throw new ServiceException("逾期费用由系统在完成时自动结算，不允许直接修改");
                }
                if (borrowRecord.getFinePaid() != null && !borrowRecord.getFinePaid().equals(old.getFinePaid()))
                {
                    throw new ServiceException("罚款缴纳请使用收款功能，不允许直接修改");
                }
                if (borrowRecord.getRenewCount() != null
                        && (old.getRenewCount() == null || !borrowRecord.getRenewCount().equals(old.getRenewCount())))
                {
                    throw new ServiceException("续期次数由系统在续期时累加，不允许直接修改");
                }
            }
        }
        // 若成员/服务被修改，同步刷新快照（保持记录显示与实际一致）
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

    /** 完成：置完成日期 + 状态已完成 + 服务库存+1（事务） */
    @Transactional
    public int returnBook(Long borrowId)
    {
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
        if (record == null)
        {
            throw new ServiceException("报名记录不存在");
        }
        if ("1".equals(record.getStatus()))
        {
            throw new ServiceException("该服务已完成，请勿重复操作");
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
            throw new ServiceException("该服务已完成，请勿重复操作");
        }
        // 库存 +1（仅状态转换成功后回补）
        if (record.getBookId() != null)
        {
            bookMapper.restoreStock(record.getBookId(), 1L);
        }
        // 完成后同步逾期催办公告：还清了就删公告，还有逾期就重新汇总
        syncOverdueNotice();
        // 完成后检查候补：该书最早的"候补中"成员 → 状态置"有名额"（CAS 推进：
        // 同书多本并发完成时同一候补只被推进一次，推进失败取下一位，队列不卡死）
        com.ruoyi.system.domain.BookReserve rq = new com.ruoyi.system.domain.BookReserve();
        rq.setBookId(record.getBookId());
        rq.setStatus("0");
        java.util.List<com.ruoyi.system.domain.BookReserve> reserves = bookReserveMapper.selectBookReserveList(rq);
        if (reserves != null && !reserves.isEmpty())
        {
            for (com.ruoyi.system.domain.BookReserve first : reserves)
            {
                // 条件更新抢推进权：只有把该候补"候补中 → 有名额"成功的请求负责通知（防双推进/双通知）
                if (bookReserveMapper.updateStatusIfCurrent(first.getReserveId(), "0", "1", new Date()) == 0)
                {
                    continue; // 该预约已被并发推进/取消，取下一位
                }
                // 书已完成 → 通知排在最前的候补成员前来取书（模板渲染、异步、尽力而为）
                Reader rv = readerMapper.selectReaderByReaderId(first.getReaderId());
                if (rv != null)
                {
                    java.util.Map<String, Object> p2 = new java.util.HashMap<>();
                    p2.put("readerName", rv.getReaderName());
                    p2.put("bookName", first.getBookName() == null ? "服务" : first.getBookName());
                    p2.put("days", reserveExpireDays());
                    mailTemplateService.send("reserve.available", rv.getEmail(), p2);
                }
                break;
            }
        }
        // 报名/罚款数据变了：失效统计缓存
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
     * 完成后同步逾期催办公告：
     * 1) 无剩余逾期记录 → 删除全部"逾期催办通知"公告
     * 2) 仍有逾期记录 → 删除旧公告并重新汇总发布（内容与现状一致）
     */
    private void syncOverdueNotice()
    {
        // 逾期状态"2"仅每日定时任务落库，未跑前真实逾期记录仍是"0"：
        // 必须按"进行中 + 截止日期已过"查询，否则会误删公告且不重建（与 BorrowTask.remindOverdue 口径一致）
        List<BorrowRecord> overdue = borrowRecordMapper.selectOverdueRecords();
        // 删除旧的催办公告
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
        // 重新汇总发布（内容与当前逾期情况一致；公告前台匿名可见，不列成员姓名，保护隐私）
        String books = "";
        int max = Math.min(overdue.size(), 5);
        for (int i = 0; i < max; i++)
        {
            BorrowRecord br = overdue.get(i);
            books += "《" + (br.getBookName() == null ? "未知" : br.getBookName()) + "》 ";
        }
        if (overdue.size() > max)
        {
            books += "等共 " + overdue.size() + " 本";
        }
        com.ruoyi.system.domain.SysNotice notice = new com.ruoyi.system.domain.SysNotice();
        notice.setNoticeTitle("逾期催还通知");
        notice.setNoticeType("2");
        notice.setNoticeContent("以下服务已截止未完成，请相关成员尽快到服务台办理：" + books);
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

    /** 续期：截止日期 +30 天（行锁：校验与写入原子化，并发双击/双端续期只有一次生效） */
    @Override
    @Transactional
    public int renewBook(Long borrowId)
    {
        // FOR UPDATE 锁记录行：上限/未逾期校验与写入原子化，并发续期不丢次数
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowIdForUpdate(borrowId);
        if (record == null)
        {
            throw new ServiceException("报名记录不存在");
        }
        if ("1".equals(record.getStatus()))
        {
            throw new ServiceException("该服务已完成，无需续期");
        }
        if ("2".equals(record.getStatus()))
        {
            throw new ServiceException("该记录已逾期，请先归还后再借");
        }
        if (record.getDueDate() == null)
        {
            throw new ServiceException("应还日期缺失，无法续期");
        }
        // 已逾期（真实日期判断）：逾期状态"2"是查询时动态算的、不落库，
        // 这里必须直接比较日期，否则逾期记录会漏过上面的 status 检查
        if (record.getDueDate().before(new Date()))
        {
            throw new ServiceException("该记录已逾期，请先归还后再借");
        }
        // 续期次数限制（委托 BorrowRuleService）
        long renewCount = borrowRuleService.checkRenewAllowed(record);
        // 截止日期 +30 天
        Date newDue = new Date(record.getDueDate().getTime() + 30L * 24 * 3600 * 1000);
        record.setDueDate(newDue);
        record.setRenewCount(renewCount + 1);
        record.setUpdateTime(new Date());
        return borrowRecordMapper.updateBorrowRecord(record);
    }

    /** 前台报名：按成员编号（匿名）
     * 必须带事务：内部自调用 insertBorrowRecord，其 @Transactional 会被 Spring 代理绕过，
     * 无事务时 FOR UPDATE 锁立即释放、检查与插入不原子（并发重复借/超上限拦不住） */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int borrowByCard(String cardNo, Long bookId)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            throw new ServiceException("请输入成员编号");
        }
        Reader reader = readerService.findActiveReader(cardNo);
        BorrowRecord borrow = new BorrowRecord();
        borrow.setReaderId(reader.getReaderId());
        borrow.setBookId(bookId);
        return insertBorrowRecord(borrow);
    }

    /** 前台续期：证号归属校验 + 进行中 + 未逾期 → 截止日期 +30 天（行锁：并发续期只有一次生效） */
    @Override
    @Transactional
    public int renewByCard(String cardNo, Long borrowId)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || borrowId == null)
        {
            throw new ServiceException("参数不完整");
        }
        // FOR UPDATE 锁记录行：上限/未逾期校验与写入原子化，并发续期不丢次数
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowIdForUpdate(borrowId);
        if (record == null)
        {
            throw new ServiceException("报名记录不存在");
        }
        // 证号归属校验：只能续期自己的书
        Reader reader = readerMapper.selectReaderByReaderId(record.getReaderId());
        if (reader == null || !cardNo.trim().equals(reader.getCardNo()))
        {
            throw new ServiceException("该报名记录不属于此证号");
        }
        if ("1".equals(record.getStatus()))
        {
            throw new ServiceException("该服务已完成，无需续期");
        }
        if (record.getDueDate() == null)
        {
            throw new ServiceException("应还日期缺失，无法续期");
        }
        // 逾期判断（真实日期，逾期状态"2"不落库）
        if (record.getDueDate().before(new Date()))
        {
            throw new ServiceException("该记录已逾期，请先归还后再借");
        }
        // 续期次数限制（与后台 renewBook 一致，委托 BorrowRuleService）
        long renewCount = borrowRuleService.checkRenewAllowed(record);
        Date newDue = new Date(record.getDueDate().getTime() + 30L * 24 * 3600 * 1000);
        record.setDueDate(newDue);
        record.setRenewCount(renewCount + 1);
        record.setUpdateTime(new Date());
        int rows = borrowRecordMapper.updateBorrowRecord(record);
        // 续期成功邮件（模板渲染、异步、尽力而为；主题"续期成功"由模板控制，修复旧复制粘贴错误）
        java.util.Map<String, Object> p3 = new java.util.HashMap<>();
        p3.put("readerName", reader.getReaderName());
        p3.put("bookName", record.getBookName() == null ? "该书" : record.getBookName());
        p3.put("dueDate", com.ruoyi.common.utils.DateUtils.parseDateToStr("yyyy-MM-dd", newDue));
        mailTemplateService.send("renew.success", reader.getEmail(), p3);
        return rows;
    }

    /** 候补名额有效天数（邮件文案 {days} 与定时任务取消逻辑一致，默认 2 天） */
    private int reserveExpireDays()
    {
        int days = 2;
        try
        {
            String v = configService.selectConfigByKey("book.reserve.expireDays");
            if (v != null && !v.isEmpty())
            {
                days = Integer.parseInt(v);
            }
        }
        catch (Exception ignore) { }
        return days < 1 ? 1 : days;
    }

    /** 删除报名记录：未还记录（进行中/逾期）先还原库存再删，有未缴罚款则拒绝（删除会抹掉欠费） */
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
            // 未缴罚款守卫（M2 修复）：对全部状态统一生效——「已完成但罚款未缴」的记录
            // 同样禁止删除（此前只拦截进行中/逾期，已完成记录可绕过守卫把欠费凭据抹掉）
            if (record.getFineAmount() != null
                    && record.getFineAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                    && com.ruoyi.system.constant.BizStatus.FINE_UNPAID.equals(record.getFinePaid()))
            {
                throw new ServiceException("该报名记录有未缴罚款，请先到服务台收款后再删除");
            }
            if (com.ruoyi.system.constant.BizStatus.BORROW_OUT.equals(record.getStatus())
                    || com.ruoyi.system.constant.BizStatus.BORROW_OVERDUE.equals(record.getStatus()))
            {
                // 动态逾期（status='0' 但已过应还日）的罚款尚未落库（仅在核销时结算）：
                // 先结算入账，否则下面的欠费拦截必然放行，删除会把逾期产生的欠费一并抹掉
                java.math.BigDecimal fineNow = fineService.calcFine(record);
                if (fineNow != null && record.getFineAmount() == null)
                {
                    record.setFineAmount(fineNow);
                    record.setFinePaid(com.ruoyi.system.constant.BizStatus.FINE_UNPAID);
                    borrowRecordMapper.updateBorrowRecord(record);
                }
                // CAS 先置"已完成"再删：并发删除同一未还记录时只有一次回补库存（防库存双回补）
                java.util.Date now = new Date();
                int locked = borrowRecordMapper.updateStatusIfCurrent(borrowId,
                        com.ruoyi.system.constant.BizStatus.BORROW_OUT,
                        com.ruoyi.system.constant.BizStatus.BORROW_RETURNED, now, null, null, now);
                if (locked == 0)
                {
                    // 逾期状态"2"为定时任务落库：再试一次；仍 0 行说明已被并发完成/删除处理
                    locked = borrowRecordMapper.updateStatusIfCurrent(borrowId,
                            com.ruoyi.system.constant.BizStatus.BORROW_OVERDUE,
                            com.ruoyi.system.constant.BizStatus.BORROW_RETURNED, now, null, null, now);
                }
                if (locked == 0)
                {
                    continue; // 已被并发处理（核销/删除），跳过不再回补
                }
                // 未完成删除 = 库存还原（书回到书架，记录作废）
                if (record.getBookId() != null)
                {
                    bookMapper.restoreStock(record.getBookId(), 1L);
                }
            }
        }
        int rows = borrowRecordMapper.deleteBorrowRecordByBorrowIds(borrowIds);
        // 报名数据变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /** 删除单条报名记录：委托批量路径（未还记录还原库存、欠费拦截与批量删除一致） */
    @Override
    @Transactional
    public int deleteBorrowRecordByBorrowId(Long borrowId)
    {
        return deleteBorrowRecordByBorrowIds(new Long[] { borrowId });
    }
}
