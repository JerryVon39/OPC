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
        System.out.println("逾期检查完成，共标记 " + count + " 条记录为逾期");
    }

    /**
     * 逾期催还公告：扫描逾期记录，若有则发布一条催还公告
     * 建议 cron：每天 9 点执行 0 0 9 * * ?
     */
    public void remindOverdue()
    {
        BorrowRecord query = new BorrowRecord();
        query.setStatus("2");
        List<BorrowRecord> overdue = borrowRecordMapper.selectBorrowRecordList(query);
        if (overdue == null || overdue.isEmpty())
        {
            System.out.println("催还检查：无逾期记录");
            return;
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
