package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.BookPurchaseReq;
import com.ruoyi.system.mapper.BookPurchaseReqMapper;
import com.ruoyi.system.service.IBookPurchaseReqService;

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
        bookPurchaseReq.setUpdateTime(DateUtils.getNowDate());
        return bookPurchaseReqMapper.updateBookPurchaseReq(bookPurchaseReq);
    }

    @Override
    public int deleteBookPurchaseReqByReqIds(Long[] reqIds)
    {
        return bookPurchaseReqMapper.deleteBookPurchaseReqByReqIds(reqIds);
    }
}