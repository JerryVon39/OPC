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
}
