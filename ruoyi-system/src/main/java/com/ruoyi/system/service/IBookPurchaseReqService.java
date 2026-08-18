package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.BookPurchaseReq;

/**
 * 图书荐购申请Service接口
 *
 * @author ruoyi
 * @date 2026-08-18
 */
public interface IBookPurchaseReqService
{
    public BookPurchaseReq selectBookPurchaseReqByReqId(Long reqId);

    public List<BookPurchaseReq> selectBookPurchaseReqList(BookPurchaseReq bookPurchaseReq);

    /** 前台匿名提交荐购申请（同书名待处理不重复提交） */
    public int applyPurchase(BookPurchaseReq bookPurchaseReq);

    public int updateBookPurchaseReq(BookPurchaseReq bookPurchaseReq);

    public int deleteBookPurchaseReqByReqIds(Long[] reqIds);
}