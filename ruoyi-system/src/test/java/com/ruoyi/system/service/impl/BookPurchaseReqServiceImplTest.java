package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BookPurchaseReq;
import com.ruoyi.system.mapper.BookPurchaseReqMapper;

/**
 * 图书荐购申请 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
class BookPurchaseReqServiceImplTest
{
    @Mock
    private BookPurchaseReqMapper bookPurchaseReqMapper;

    @InjectMocks
    private BookPurchaseReqServiceImpl bookPurchaseReqService;

    @Test
    void applyPurchase_success_trimsAndInsertsPending()
    {
        BookPurchaseReq req = new BookPurchaseReq();
        req.setBookName("  三体 外传 ");
        req.setAuthor(" 刘慈欣 ");
        when(bookPurchaseReqMapper.countPendingByName("三体 外传")).thenReturn(0);
        when(bookPurchaseReqMapper.insertBookPurchaseReq(any(BookPurchaseReq.class))).thenReturn(1);

        int rows = bookPurchaseReqService.applyPurchase(req);

        assertEquals(1, rows);
        assertEquals("三体 外传", req.getBookName()); // 去首尾空格
        assertEquals("0", req.getStatus());           // 默认待处理
        assertNotNull(req.getCreateTime());
        verify(bookPurchaseReqMapper).insertBookPurchaseReq(req);
    }

    @Test
    void applyPurchase_emptyBookName_throws()
    {
        BookPurchaseReq req = new BookPurchaseReq();
        req.setBookName("   ");

        assertThrows(ServiceException.class, () -> bookPurchaseReqService.applyPurchase(req));
        verify(bookPurchaseReqMapper, never()).insertBookPurchaseReq(any(BookPurchaseReq.class));
    }

    @Test
    void applyPurchase_duplicatePending_throws()
    {
        BookPurchaseReq req = new BookPurchaseReq();
        req.setBookName("三体");
        when(bookPurchaseReqMapper.countPendingByName("三体")).thenReturn(1);

        assertThrows(ServiceException.class, () -> bookPurchaseReqService.applyPurchase(req));
        verify(bookPurchaseReqMapper, never()).insertBookPurchaseReq(any(BookPurchaseReq.class));
    }
}