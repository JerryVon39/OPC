package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.BookPurchaseReq;

/**
 * 服务入驻申请申请Mapper接口
 *
 * @author ruoyi
 * @date 2026-08-18
 */
public interface BookPurchaseReqMapper
{
    public BookPurchaseReq selectBookPurchaseReqByReqId(Long reqId);

    public List<BookPurchaseReq> selectBookPurchaseReqList(BookPurchaseReq bookPurchaseReq);

    /** 去重计数：同一书名且"待处理"状态的申请数（前台提交防重复） */
    public int countPendingByName(String bookName);

    public int insertBookPurchaseReq(BookPurchaseReq bookPurchaseReq);

    public int updateBookPurchaseReq(BookPurchaseReq bookPurchaseReq);

    public int deleteBookPurchaseReqByReqIds(Long[] reqIds);
}