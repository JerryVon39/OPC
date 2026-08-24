package com.ruoyi.system.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.system.domain.ReaderLoginLog;
import com.ruoyi.system.mapper.ReaderLoginLogMapper;
import com.ruoyi.system.service.IReaderLoginLogService;

/**
 * 登录审计实现：写库失败仅记日志（审计不能拖垮登录主流程）
 */
@Service
public class ReaderLoginLogServiceImpl implements IReaderLoginLogService
{
    private static final Logger log = LoggerFactory.getLogger(ReaderLoginLogServiceImpl.class);

    @Autowired
    private ReaderLoginLogMapper readerLoginLogMapper;

    @Override
    public void log(String cardNo, Long readerId, String ip, String event, boolean success, String msg)
    {
        try
        {
            ReaderLoginLog l = new ReaderLoginLog();
            l.setCardNo(cardNo == null ? "" : cardNo);
            l.setReaderId(readerId);
            l.setIp(ip == null ? "" : ip);
            l.setEvent(event);
            l.setResult(success ? "1" : "0");
            l.setMsg(msg == null ? "" : msg);
            readerLoginLogMapper.insertReaderLoginLog(l);
        }
        catch (Exception e)
        {
            log.warn("登录审计写入失败（不影响业务）：{}", e.getMessage());
        }
    }

    @Override
    public List<ReaderLoginLog> list(ReaderLoginLog query)
    {
        return readerLoginLogMapper.selectReaderLoginLogList(query);
    }
}
