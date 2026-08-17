package com.ruoyi.quartz.task;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;

/**
 * 借阅相关定时任务
 * 
 * 使用方式（若依定时任务页面）：目标字符串填 borrowTask.updateOverdueStatus()
 */
@Component("borrowTask")
public class BorrowTask
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private com.ruoyi.system.mapper.SysNoticeMapper noticeMapper;

    @Autowired
    private com.ruoyi.system.mapper.SysUserMapper userMapper;

    @Autowired
    private com.ruoyi.system.mapper.BookReserveMapper reserveMapper;

    @Autowired
    private com.ruoyi.system.service.ISysConfigService configService;

    @Autowired
    private com.ruoyi.system.service.StatisticsService statisticsService;

    /**
     * 逾期自动标记：将"借出中"且应还日期已过的记录持久化为"已逾期"(2)
     * 建议 cron：每天 0 点执行 0 0 0 * * ?
     */
    public void updateOverdueStatus()
    {
        BorrowRecord query = new BorrowRecord();
        query.setStatus("0");
        List<BorrowRecord> list = borrowRecordMapper.selectBorrowRecordList(query);
        Date now = new Date();
        int count = 0;
        for (BorrowRecord br : list)
        {
            if (br.getDueDate() != null && br.getDueDate().before(now))
            {
                br.setStatus("2");
                br.setUpdateTime(now);
                borrowRecordMapper.updateBorrowRecord(br);
                count++;
            }
        }
        // 逾期状态影响看板口径（borrowingCount/overdueCount）：有变更就失效统计缓存
        if (count > 0)
        {
            statisticsService.evictAll();
        }
        System.out.println("逾期检查完成，共标记 " + count + " 条记录为逾期");
    }

    /**
     * 预约超时检查：'可借'状态超过 N 天未到馆借阅 → 自动取消并通知下一位预约人
     * 建议 cron：每天 8 点执行 0 0 8 * * ?
     */
    public void reserveExpireCheck()
    {
        int expireDays = 2;
        try
        {
            String v = configService.selectConfigByKey("book.reserve.expireDays");
            if (v != null && !v.isEmpty())
            {
                expireDays = Integer.parseInt(v);
            }
        }
        catch (Exception ignore) { }
        com.ruoyi.system.domain.BookReserve q = new com.ruoyi.system.domain.BookReserve();
        q.setStatus("1");
        java.util.List<com.ruoyi.system.domain.BookReserve> list = reserveMapper.selectBookReserveList(q);
        if (list == null || list.isEmpty())
        {
            System.out.println("预约超时检查：无可借待取预约");
            return;
        }
        Date now = new Date();
        int count = 0;
        for (com.ruoyi.system.domain.BookReserve r : list)
        {
            if (r.getUpdateTime() == null
                    || now.getTime() - r.getUpdateTime().getTime() <= expireDays * 24L * 3600 * 1000)
            {
                continue;
            }
            // 超时未取：取消该预约
            r.setStatus("3");
            r.setUpdateTime(now);
            reserveMapper.updateBookReserve(r);
            count++;
            // 通知下一位预约人（该书最早的'预约中'）
            com.ruoyi.system.domain.BookReserve nq = new com.ruoyi.system.domain.BookReserve();
            nq.setBookId(r.getBookId());
            nq.setStatus("0");
            java.util.List<com.ruoyi.system.domain.BookReserve> next = reserveMapper.selectBookReserveList(nq);
            if (next != null && !next.isEmpty())
            {
                com.ruoyi.system.domain.BookReserve n = next.get(0);
                n.setStatus("1");
                n.setUpdateTime(now);
                reserveMapper.updateBookReserve(n);
            }
        }
        System.out.println("预约超时检查完成，共取消 " + count + " 条超时可借预约");
    }

    /**
     * 逾期催还公告：扫描逾期记录，若有则发布一条催还公告
     * 建议 cron：每天 9 点执行 0 0 9 * * ?
     */
    public void remindOverdue()
    {
        // 同时查"已逾期(2)"和"借出中(0)"，再按真实日期过滤：
        // 即使 0 点的逾期标记任务因服务宕机没跑，9 点也能发现真正逾期的记录
        BorrowRecord query = new BorrowRecord();
        query.setStatus("2");
        List<BorrowRecord> overdue = borrowRecordMapper.selectBorrowRecordList(query);
        query.setStatus("0");
        List<BorrowRecord> borrowing = borrowRecordMapper.selectBorrowRecordList(query);
        Date now = new Date();
        if (borrowing != null)
        {
            for (BorrowRecord br : borrowing)
            {
                if (br.getDueDate() != null && br.getDueDate().before(now))
                {
                    overdue.add(br);
                }
            }
        }
        if (overdue == null || overdue.isEmpty())
        {
            System.out.println("催还检查：无逾期记录");
            return;
        }
        // 去重：今天已发布过催还公告则跳过（避免同一条逾期记录每天重复催收）
        com.ruoyi.system.domain.SysNotice sentQuery = new com.ruoyi.system.domain.SysNotice();
        sentQuery.setNoticeTitle("逾期催还通知");
        java.util.List<com.ruoyi.system.domain.SysNotice> sentList = noticeMapper.selectNoticeList(sentQuery);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        for (com.ruoyi.system.domain.SysNotice sn : sentList)
        {
            if (sn.getCreateTime() != null && today.equals(sdf.format(sn.getCreateTime())))
            {
                System.out.println("催还检查：今日已发布过催还公告，跳过");
                return;
            }
        }
        // 汇总逾期信息
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
        System.out.println("催还公告已发布，涉及 " + overdue.size() + " 条逾期记录");
    }
}
