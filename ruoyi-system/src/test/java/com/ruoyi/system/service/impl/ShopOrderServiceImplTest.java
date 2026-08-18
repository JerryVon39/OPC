package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.StatisticsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 购书订单单元测试：下单校验链（读者/图书/库存/扣减）+ 取消回滚库存 + 统计缓存失效
 */
@ExtendWith(MockitoExtension.class)
public class ShopOrderServiceImplTest
{
    @Mock
    private ShopOrderMapper shopOrderMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ReaderMapper readerMapper;

    @Mock
    private IReaderService readerService;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private ShopOrderServiceImpl shopOrderService;

    private Reader reader;
    private Book book;

    @BeforeEach
    void setUp()
    {
        reader = new Reader();
        reader.setReaderId(2L);
        reader.setReaderName("测试读者");
        reader.setCardNo("JS12345678");
        reader.setStatus("0");

        book = new Book();
        book.setBookId(3L);
        book.setBookName("三体");
        book.setPrice(new BigDecimal("59.60"));
        book.setStatus("0");
        book.setStock(5L);
    }

    /** 证号为空 → "请先登录" */
    @Test
    void createOrder_noCard_throws()
    {
        ServiceException e = assertThrows(ServiceException.class, () -> shopOrderService.createOrder("", 3L, 1L));
        assertTrue(e.getMessage().contains("请先登录"));
    }

    /** 读者停用 → 抛异常 */
    @Test
    void createOrder_readerDisabled_throws()
    {
        reader.setStatus("1");
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        ServiceException e = assertThrows(ServiceException.class,
                () -> shopOrderService.createOrder("JS12345678", 3L, 1L));
        assertTrue(e.getMessage().contains("停用/挂失"));
    }

    /** 图书下架 → 抛异常 */
    @Test
    void createOrder_bookOffSale_throws()
    {
        book.setStatus("1");
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookId(3L)).thenReturn(book);
        assertThrows(ServiceException.class, () -> shopOrderService.createOrder("JS12345678", 3L, 1L));
    }

    /** 库存不足 → 抛异常 */
    @Test
    void createOrder_insufficientStock_throws()
    {
        book.setStock(1L);
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookId(3L)).thenReturn(book);
        ServiceException e = assertThrows(ServiceException.class,
                () -> shopOrderService.createOrder("JS12345678", 3L, 2L));
        assertTrue(e.getMessage().contains("库存不足"));
    }

    /** 读者在 findActiveReader 与加锁之间被并发删除 → 明确"读者不存在"而非 NPE */
    @Test
    void createOrder_readerDeletedConcurrently_throws()
    {
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(null);
        ServiceException e = assertThrows(ServiceException.class,
                () -> shopOrderService.createOrder("JS12345678", 3L, 1L));
        assertTrue(e.getMessage().contains("读者不存在"));
    }

    /** 并发兜底：updateStock 返回 0（期间库存被抢光）→ 抛异常 */
    @Test
    void createOrder_concurrentStockLoss_throws()
    {
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookId(3L)).thenReturn(book);
        when(bookMapper.updateStock(3L, 1L)).thenReturn(0);
        assertThrows(ServiceException.class, () -> shopOrderService.createOrder("JS12345678", 3L, 1L));
    }

    /** 下单成功：订单号 WSW 前缀、快照完整、扣库存、失效统计缓存 */
    @Test
    void createOrder_success()
    {
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookId(3L)).thenReturn(book);
        when(bookMapper.updateStock(3L, 1L)).thenReturn(1);
        when(shopOrderMapper.insertShopOrder(any(ShopOrder.class))).thenReturn(1);

        assertEquals(1, shopOrderService.createOrder("JS12345678", 3L, null));
        verify(shopOrderMapper).insertShopOrder(argThat(o ->
                o.getOrderNo().startsWith("WSW")
                        && "0".equals(o.getStatus())
                        && o.getQuantity() == 1L
                        && o.getReaderName().equals("测试读者")));
        verify(statisticsService).evictAll();
    }

    /** 前台取消：证号不符 → 抛 */
    @Test
    void cancelByCard_wrongCard_throws()
    {
        ShopOrder order = new ShopOrder();
        order.setOrderId(1L);
        order.setCardNo("JS99999999");
        order.setStatus("0");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(order);
        ServiceException e = assertThrows(ServiceException.class,
                () -> shopOrderService.cancelByCard("JS12345678", 1L));
        assertTrue(e.getMessage().contains("不属于此证号"));
    }

    /** 前台取消：已处理订单 → 抛 */
    @Test
    void cancelByCard_processed_throws()
    {
        ShopOrder order = new ShopOrder();
        order.setOrderId(1L);
        order.setCardNo("JS12345678");
        order.setStatus("1");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(order);
        assertThrows(ServiceException.class, () -> shopOrderService.cancelByCard("JS12345678", 1L));
    }

    /** 前台取消：成功 → 回滚库存 + 状态置已取消 + 失效统计缓存 */
    @Test
    void cancelByCard_success_restoresStock()
    {
        ShopOrder order = new ShopOrder();
        order.setOrderId(1L);
        order.setCardNo("JS12345678");
        order.setStatus("0");
        order.setBookId(3L);
        order.setQuantity(2L);
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(order);
        when(shopOrderMapper.updateStatusIfCurrent(1L, "0", "2")).thenReturn(1);

        assertEquals(1, shopOrderService.cancelByCard("JS12345678", 1L));
        verify(bookMapper).restoreStock(3L, 2L);
        verify(statisticsService).evictAll();
    }

    /** 前台取消：并发下状态已被其他请求处理 → 不回滚库存 */
    @Test
    void cancelByCard_concurrentStateLoss_noRestore()
    {
        ShopOrder order = new ShopOrder();
        order.setOrderId(1L);
        order.setCardNo("JS12345678");
        order.setStatus("0");
        order.setBookId(3L);
        order.setQuantity(2L);
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(order);
        when(shopOrderMapper.updateStatusIfCurrent(1L, "0", "2")).thenReturn(0);

        assertThrows(ServiceException.class, () -> shopOrderService.cancelByCard("JS12345678", 1L));
        verify(bookMapper, never()).restoreStock(any(), any());
    }

    /** 后台取消：非待付款 → 抛 */
    @Test
    void updateShopOrder_cancelNonPending_throws()
    {
        ShopOrder old = new ShopOrder();
        old.setStatus("1");
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("2");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        assertThrows(ServiceException.class, () -> shopOrderService.updateShopOrder(update));
    }

    /** 后台取消：待付款 → 回滚库存 */
    @Test
    void updateShopOrder_cancelPending_restoresStock()
    {
        ShopOrder old = new ShopOrder();
        old.setOrderId(1L);
        old.setStatus("0");
        old.setBookId(3L);
        old.setQuantity(1L);
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("2");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        when(shopOrderMapper.updateShopOrder(update)).thenReturn(1);

        assertEquals(1, shopOrderService.updateShopOrder(update));
        verify(bookMapper).restoreStock(3L, 1L);
    }

    /** 后台收款：待付款 → 已收款，合法流转（不动库存） */
    @Test
    void updateShopOrder_payFromPending_ok()
    {
        ShopOrder old = new ShopOrder();
        old.setOrderId(1L);
        old.setStatus("0");
        old.setBookId(3L);
        old.setQuantity(1L);
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("3");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        when(shopOrderMapper.updateShopOrder(update)).thenReturn(1);

        assertEquals(1, shopOrderService.updateShopOrder(update));
        verify(bookMapper, never()).restoreStock(any(), any());
    }

    /** 后台完成：已收款 → 已完成，合法流转（不动库存） */
    @Test
    void updateShopOrder_completeFromPaid_ok()
    {
        ShopOrder old = new ShopOrder();
        old.setOrderId(1L);
        old.setStatus("3");
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("1");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        when(shopOrderMapper.updateShopOrder(update)).thenReturn(1);

        assertEquals(1, shopOrderService.updateShopOrder(update));
        verify(bookMapper, never()).restoreStock(any(), any());
    }

    /** 后台取消：已收款订单 → 抛（收款后不允许退款改单） */
    @Test
    void updateShopOrder_cancelFromPaid_throws()
    {
        ShopOrder old = new ShopOrder();
        old.setOrderId(1L);
        old.setStatus("3");
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("2");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        assertThrows(ServiceException.class, () -> shopOrderService.updateShopOrder(update));
    }

    /** 后台收款：已取消订单 → 抛（库存已回补，不能再收款） */
    @Test
    void updateShopOrder_payFromCancelled_throws()
    {
        ShopOrder old = new ShopOrder();
        old.setOrderId(1L);
        old.setStatus("2");
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("3");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        assertThrows(ServiceException.class, () -> shopOrderService.updateShopOrder(update));
    }

    /** 后台改单：已完成订单再改状态 → 抛 */
    @Test
    void updateShopOrder_editFromCompleted_throws()
    {
        ShopOrder old = new ShopOrder();
        old.setOrderId(1L);
        old.setStatus("1");
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        update.setStatus("3");
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(old);
        assertThrows(ServiceException.class, () -> shopOrderService.updateShopOrder(update));
    }

    /** 后台改单：订单不存在 → 抛明确异常 */
    @Test
    void updateShopOrder_orderNotFound_throws()
    {
        ShopOrder update = new ShopOrder();
        update.setOrderId(99L);
        update.setStatus("3");
        when(shopOrderMapper.selectShopOrderByOrderId(99L)).thenReturn(null);
        ServiceException e = assertThrows(ServiceException.class,
                () -> shopOrderService.updateShopOrder(update));
        assertTrue(e.getMessage().contains("订单不存在"));
    }

    /** 后台改单：status 为空 → 抛 */
    @Test
    void updateShopOrder_nullStatus_throws()
    {
        ShopOrder update = new ShopOrder();
        update.setOrderId(1L);
        ServiceException e = assertThrows(ServiceException.class,
                () -> shopOrderService.updateShopOrder(update));
        assertTrue(e.getMessage().contains("参数不完整"));
    }

    /** 删除订单：待付款(0) → 还原库存后删 + 失效缓存 */
    @Test
    void deleteShopOrderByOrderIds_pending_restoresStock()
    {
        ShopOrder order = new ShopOrder();
        order.setOrderId(1L);
        order.setStatus("0");
        order.setBookId(3L);
        order.setQuantity(2L);
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(order);
        when(shopOrderMapper.deleteShopOrderByOrderIds(any())).thenReturn(1);

        assertEquals(1, shopOrderService.deleteShopOrderByOrderIds(new Long[] { 1L }));
        verify(bookMapper).restoreStock(3L, 2L);
        verify(statisticsService).evictAll();
    }

    /** 删除订单：已完成(1) → 直接删，不还原库存 */
    @Test
    void deleteShopOrderByOrderIds_completed_noRestore()
    {
        ShopOrder order = new ShopOrder();
        order.setOrderId(1L);
        order.setStatus("1");
        order.setBookId(3L);
        order.setQuantity(1L);
        when(shopOrderMapper.selectShopOrderByOrderId(1L)).thenReturn(order);
        when(shopOrderMapper.deleteShopOrderByOrderIds(any())).thenReturn(1);

        assertEquals(1, shopOrderService.deleteShopOrderByOrderIds(new Long[] { 1L }));
        verify(bookMapper, never()).restoreStock(any(), any());
    }
}
