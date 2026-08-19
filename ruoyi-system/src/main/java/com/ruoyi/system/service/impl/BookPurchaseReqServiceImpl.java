package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.BookPurchaseReq;
import com.ruoyi.system.mapper.BookPurchaseReqMapper;
import com.ruoyi.system.service.IBookPurchaseReqService;
import com.ruoyi.common.utils.MailUtil;

/**
 * 图书荐购申请Service业务层处理
 *
 * @author ruoyi
 * @date 2026-08-18
 */
@Service
public class BookPurchaseReqServiceImpl implements IBookPurchaseReqService
{
    @Autowired
    private BookPurchaseReqMapper bookPurchaseReqMapper;

    @Autowired
    private MailUtil mailUtil;

    @Override
    public BookPurchaseReq selectBookPurchaseReqByReqId(Long reqId)
    {
        return bookPurchaseReqMapper.selectBookPurchaseReqByReqId(reqId);
    }

    @Override
    public List<BookPurchaseReq> selectBookPurchaseReqList(BookPurchaseReq bookPurchaseReq)
    {
        return bookPurchaseReqMapper.selectBookPurchaseReqList(bookPurchaseReq);
    }

    /**
     * 前台匿名提交荐购申请：
     * 书名必填；同一书名存在"待处理"申请时不重复提交（防刷屏）
     */
    @Override
    public int applyPurchase(BookPurchaseReq req)
    {
        if (req.getBookName() == null || req.getBookName().trim().isEmpty())
        {
            throw new com.ruoyi.common.exception.ServiceException("请填写书名");
        }
        // 申请者邮箱必填且格式合法（荐购结果需邮件通知）
        if (req.getEmail() == null || !req.getEmail().trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$") || req.getEmail().trim().length() > 50)
        {
            throw new com.ruoyi.common.exception.ServiceException("请填写有效的电子邮箱（处理结果将邮件通知）");
        }
        req.setEmail(req.getEmail().trim());
        req.setBookName(req.getBookName().trim());
        if (req.getAuthor() != null) req.setAuthor(req.getAuthor().trim());
        if (req.getRemark() != null) req.setRemark(req.getRemark().trim());
        if (bookPurchaseReqMapper.countPendingByName(req.getBookName()) > 0)
        {
            throw new com.ruoyi.common.exception.ServiceException("《" + req.getBookName() + "》已在待处理荐购中，请勿重复提交");
        }
        req.setStatus("0");
        req.setCreateTime(DateUtils.getNowDate());
        return bookPurchaseReqMapper.insertBookPurchaseReq(req);
    }

    @Override
    public int updateBookPurchaseReq(BookPurchaseReq bookPurchaseReq)
    {
        // 处理荐购（待处理→已处理/已拒绝）时，向后留的申请者邮箱发结果通知
        BookPurchaseReq old = bookPurchaseReq.getReqId() == null ? null
                : bookPurchaseReqMapper.selectBookPurchaseReqByReqId(bookPurchaseReq.getReqId());
        bookPurchaseReq.setUpdateTime(DateUtils.getNowDate());
        int rows = bookPurchaseReqMapper.updateBookPurchaseReq(bookPurchaseReq);
        if (rows > 0 && old != null && "0".equals(old.getStatus())
                && ("1".equals(bookPurchaseReq.getStatus()) || "2".equals(bookPurchaseReq.getStatus())))
        {
            String to = old.getEmail();
            if ("1".equals(bookPurchaseReq.getStatus()))
            {
                mailUtil.sendHtml(to, "【荐购结果】您的荐购已通过",
                        "<p>您好：</p><p>您荐购的《" + esc(old.getBookName()) + "》已通过审核，我们将尽快采购上架，欢迎届时到店借阅。感谢使用读书当铺！</p>");
            }
            else
            {
                mailUtil.sendHtml(to, "【荐购结果】您的荐购未通过",
                        "<p>您好：</p><p>很遗憾，您荐购的《" + esc(old.getBookName()) + "》暂未通过审核。我们会持续关注您的需求，感谢支持！</p>");
            }
        }
        return rows;
    }

    /** HTML 转义 */
    private String esc(String s)
    {
        if (s == null) { return ""; }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public int deleteBookPurchaseReqByReqIds(Long[] reqIds)
    {
        return bookPurchaseReqMapper.deleteBookPurchaseReqByReqIds(reqIds);
    }
}