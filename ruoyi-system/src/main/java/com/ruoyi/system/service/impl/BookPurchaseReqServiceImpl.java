package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.BookPurchaseReq;
import com.ruoyi.system.mapper.BookPurchaseReqMapper;
import com.ruoyi.system.service.IBookPurchaseReqService;
import com.ruoyi.system.service.IMailTemplateService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 服务入驻申请申请Service业务层处理
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
    private IMailTemplateService mailTemplateService;

    @Autowired
    private ISysConfigService configService;

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
     * 前台匿名提交入驻申请申请：
     * 书名必填；同一书名存在"待处理"申请时不重复提交（防刷屏）
     */
    @Override
    public int applyPurchase(BookPurchaseReq req)
    {
        if (req.getBookName() == null || req.getBookName().trim().isEmpty())
        {
            throw new com.ruoyi.common.exception.ServiceException("请填写项目/组织名称");
        }
        // 申请者邮箱必填且格式合法（入驻申请结果需邮件通知）
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
            throw new com.ruoyi.common.exception.ServiceException("《" + req.getBookName() + "》已有待审核的入驻申请，请勿重复提交");
        }
        req.setStatus("0");
        req.setCreateTime(DateUtils.getNowDate());
        int rows = bookPurchaseReqMapper.insertBookPurchaseReq(req);
        // 65：新申请邮件通知运营者（sys_config 键 opc.apply.notify.email，留空=不通知；
        // 尽力而为：邮件配置缺失/发送失败不影响申请提交）
        try
        {
            String adminEmail = configService.selectConfigByKey("opc.apply.notify.email");
            if (adminEmail != null && !adminEmail.trim().isEmpty() && rows > 0)
            {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("applyName", req.getBookName());
                params.put("contact", req.getAuthor() == null ? "" : req.getAuthor());
                params.put("email", req.getEmail());
                params.put("remark", req.getRemark() == null ? "" : req.getRemark());
                mailTemplateService.send("apply.notify", adminEmail.trim(), params);
            }
        }
        catch (Exception ignored) { }
        return rows;
    }

    @Override
    public int updateBookPurchaseReq(BookPurchaseReq bookPurchaseReq)
    {
        // 处理入驻申请（待处理→已处理/已拒绝）时，向后留的申请者邮箱发结果通知
        BookPurchaseReq old = bookPurchaseReq.getReqId() == null ? null
                : bookPurchaseReqMapper.selectBookPurchaseReqByReqId(bookPurchaseReq.getReqId());
        bookPurchaseReq.setUpdateTime(DateUtils.getNowDate());
        int rows = bookPurchaseReqMapper.updateBookPurchaseReq(bookPurchaseReq);
        if (rows > 0 && old != null && "0".equals(old.getStatus())
                && ("1".equals(bookPurchaseReq.getStatus()) || "2".equals(bookPurchaseReq.getStatus())))
        {
            String to = old.getEmail();
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("applyName", old.getBookName());
            if ("1".equals(bookPurchaseReq.getStatus()))
            {
                mailTemplateService.send("purchase.pass", to, params);
            }
            else
            {
                mailTemplateService.send("purchase.reject", to, params);
            }
        }
        return rows;
    }

    // 邮件占位符转义由 MailTemplateServiceImpl 渲染时统一处理，此处不再需要 esc

    @Override
    public int deleteBookPurchaseReqByReqIds(Long[] reqIds)
    {
        return bookPurchaseReqMapper.deleteBookPurchaseReqByReqIds(reqIds);
    }
}