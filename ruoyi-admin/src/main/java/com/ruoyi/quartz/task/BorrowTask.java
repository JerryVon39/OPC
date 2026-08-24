package com.ruoyi.quartz.task;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.system.service.IMailTemplateService;

/**
 * 报名相关定时任务
 *
 * 使用方式（若依定时任务页面）：目标字符串填 borrowTask.reserveExpireCheck()
 */
@Component("borrowTask")
public class BorrowTask
{
    @Autowired
    private com.ruoyi.system.mapper.BookReserveMapper reserveMapper;

    @Autowired
    private com.ruoyi.system.mapper.ReaderMapper readerMapper;

    @Autowired
    private com.ruoyi.system.service.ISysConfigService configService;

    @Autowired
    private IMailTemplateService mailTemplateService;

    /**
     * 候补超时检查：'有名额'状态超过 N 天未到馆报名 → 自动取消并通知下一位候补人
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
        // 配置下限：0/负数会全量取消"有名额"候补，至少 1 天
        if (expireDays < 1)
        {
            expireDays = 1;
        }
        com.ruoyi.system.domain.BookReserve q = new com.ruoyi.system.domain.BookReserve();
        q.setStatus("1");
        java.util.List<com.ruoyi.system.domain.BookReserve> list = reserveMapper.selectBookReserveList(q);
        if (list == null || list.isEmpty())
        {
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
            // 超时未取：CAS 取消（0 行说明已被并发推进/取消，不计入本次取消数）
            if (reserveMapper.updateStatusIfCurrent(r.getReserveId(), "1", "3", now) == 0)
            {
                continue;
            }
            count++;
            // 通知被取消者：名额已释放（模板渲染、异步、尽力而为；与 returnBook 通知行为一致）
            com.ruoyi.system.domain.Reader canceled = readerMapper.selectReaderByReaderId(r.getReaderId());
            if (canceled != null)
            {
                java.util.Map<String, Object> cp = new java.util.HashMap<>();
                cp.put("readerName", canceled.getReaderName());
                cp.put("bookName", r.getBookName() == null ? "服务" : r.getBookName());
                mailTemplateService.send("reserve.cancel", canceled.getEmail(), cp);
            }
            // 通知下一位候补人（该书最早的'候补中'，CAS 推进，失败取下一位）
            com.ruoyi.system.domain.BookReserve nq = new com.ruoyi.system.domain.BookReserve();
            nq.setBookId(r.getBookId());
            nq.setStatus("0");
            java.util.List<com.ruoyi.system.domain.BookReserve> next = reserveMapper.selectBookReserveList(nq);
            if (next != null)
            {
                for (com.ruoyi.system.domain.BookReserve n : next)
                {
                    if (reserveMapper.updateStatusIfCurrent(n.getReserveId(), "0", "1", now) > 0)
                    {
                        // 推进成功：通知"有名额"（与 returnBook 完成路径一致）
                        com.ruoyi.system.domain.Reader promoted = readerMapper.selectReaderByReaderId(n.getReaderId());
                        if (promoted != null)
                        {
                            java.util.Map<String, Object> pp = new java.util.HashMap<>();
                            pp.put("readerName", promoted.getReaderName());
                            pp.put("bookName", r.getBookName() == null ? "服务" : r.getBookName());
                            pp.put("days", expireDays);
                            mailTemplateService.send("reserve.available", promoted.getEmail(), pp);
                        }
                        break; // 推进成功，通知权归这次更新
                    }
                }
            }
        }
    }

}
