package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.BookReserve;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BookReserveMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.service.IReaderService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 图书预约单元测试：前置校验链（读者/图书/库存/已借/重复）+ 取消预约
 */
@ExtendWith(MockitoExtension.class)
public class BookReserveServiceImplTest
{
    @Mock
    private BookReserveMapper bookReserveMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ReaderMapper readerMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private IReaderService readerService;

    @Mock
    private com.ruoyi.common.utils.MailUtil mailUtil;

    @InjectMocks
    private BookReserveServiceImpl bookReserveService;

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
        book.setStatus("0");
        book.setStock(0L);
    }

    /** 参数不完整 → 抛异常 */
    @Test
    void reserveByCard_missingParam_throws()
    {
        assertThrows(ServiceException.class, () -> bookReserveService.reserveByCard("", 3L));
        assertThrows(ServiceException.class, () -> bookReserveService.reserveByCard("JS12345678", null));
    }

    /** 证号不存在（findActiveReader 抛）→ 传播异常 */
    @Test
    void reserveByCard_readerNotFound_throws()
    {
        when(readerService.findActiveReader("JS00000000"))
                .thenThrow(new ServiceException("借书证号不存在，请先登记"));
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS00000000", 3L));
        assertTrue(e.getMessage().contains("不存在"));
    }

    /** 读者停用 → 抛异常 */
    @Test
    void reserveByCard_readerDisabled_throws()
    {
        reader.setStatus("1");
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS12345678", 3L));
        assertTrue(e.getMessage().contains("停用/挂失"));
    }

    /** 图书下架 → 抛异常 */
    @Test
    void reserveByCard_bookOffSale_throws()
    {
        book.setStatus("1");
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookIdForUpdate(3L)).thenReturn(book);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS12345678", 3L));
        assertTrue(e.getMessage().contains("不存在或已下架"));
    }

    /** 有库存 → 提示直接借阅，无需预约 */
    @Test
    void reserveByCard_bookInStock_throws()
    {
        book.setStock(5L);
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookIdForUpdate(3L)).thenReturn(book);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS12345678", 3L));
        assertTrue(e.getMessage().contains("有库存"));
    }

    /** 已借未还 → 不可预约 */
    @Test
    void reserveByCard_alreadyBorrowing_throws()
    {
        BorrowRecord br = new BorrowRecord();
        br.setStatus("0");
        List<BorrowRecord> list = new ArrayList<>();
        list.add(br);
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookIdForUpdate(3L)).thenReturn(book);
        when(borrowRecordMapper.selectBorrowRecordList(any())).thenReturn(list);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS12345678", 3L));
        assertTrue(e.getMessage().contains("已借阅本书"));
    }

    /** 重复预约（预约中）→ 抛异常 */
    @Test
    void reserveByCard_duplicateReserve_throws()
    {
        BookReserve exist = new BookReserve();
        exist.setStatus("0");
        List<BookReserve> list = new ArrayList<>();
        list.add(exist);
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookIdForUpdate(3L)).thenReturn(book);
        when(borrowRecordMapper.selectBorrowRecordList(any())).thenReturn(new ArrayList<>());
        when(bookReserveMapper.selectBookReserveList(any())).thenReturn(list);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS12345678", 3L));
        assertTrue(e.getMessage().contains("请勿重复预约"));
    }

    /** 读者在 findActiveReader 与加锁之间被并发删除 → 明确"读者不存在"而非 NPE */
    @Test
    void reserveByCard_readerDeletedConcurrently_throws()
    {
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(null);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.reserveByCard("JS12345678", 3L));
        assertTrue(e.getMessage().contains("读者不存在"));
    }

    /** 成功预约：快照字段完整 + 状态预约中 */
    @Test
    void reserveByCard_success()
    {
        when(readerService.findActiveReader("JS12345678")).thenReturn(reader);
        when(readerMapper.selectReaderByReaderIdForUpdate(2L)).thenReturn(reader);
        when(bookMapper.selectBookByBookIdForUpdate(3L)).thenReturn(book);
        when(borrowRecordMapper.selectBorrowRecordList(any())).thenReturn(new ArrayList<>());
        when(bookReserveMapper.selectBookReserveList(any())).thenReturn(new ArrayList<>());
        when(bookReserveMapper.insertBookReserve(any(BookReserve.class))).thenReturn(1);

        assertEquals(1, bookReserveService.reserveByCard("JS12345678", 3L));
        verify(bookReserveMapper).insertBookReserve(any(BookReserve.class));
    }

    /** 取消预约：记录不存在 → 抛 */
    @Test
    void cancelByCard_notFound_throws()
    {
        when(bookReserveMapper.selectBookReserveByReserveId(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> bookReserveService.cancelByCard("JS12345678", 1L));
    }

    /** 取消预约：证号不符 → 抛 */
    @Test
    void cancelByCard_wrongCard_throws()
    {
        BookReserve reserve = new BookReserve();
        reserve.setCardNo("JS99999999");
        reserve.setStatus("0");
        when(bookReserveMapper.selectBookReserveByReserveId(1L)).thenReturn(reserve);
        ServiceException e = assertThrows(ServiceException.class,
                () -> bookReserveService.cancelByCard("JS12345678", 1L));
        assertTrue(e.getMessage().contains("不属于此证号"));
    }

    /** 取消预约：已结束状态 → 抛 */
    @Test
    void cancelByCard_finishedStatus_throws()
    {
        BookReserve reserve = new BookReserve();
        reserve.setCardNo("JS12345678");
        reserve.setStatus("2");
        when(bookReserveMapper.selectBookReserveByReserveId(1L)).thenReturn(reserve);
        assertThrows(ServiceException.class, () -> bookReserveService.cancelByCard("JS12345678", 1L));
    }

    /** 取消预约：成功 → 状态置已取消(3) */
    @Test
    void cancelByCard_success()
    {
        BookReserve reserve = new BookReserve();
        reserve.setCardNo("JS12345678");
        reserve.setStatus("1");
        when(bookReserveMapper.selectBookReserveByReserveId(1L)).thenReturn(reserve);
        assertEquals(bookReserveMapper.updateBookReserve(reserve), bookReserveService.cancelByCard("JS12345678", 1L));
        assertEquals("3", reserve.getStatus());
    }

    /** 取消预约：参数缺失 → 抛（匿名接口空参守卫，防 NPE 变 500） */
    @Test
    void cancelByCard_nullParam_throws()
    {
        assertThrows(ServiceException.class, () -> bookReserveService.cancelByCard(null, 1L));
        assertThrows(ServiceException.class, () -> bookReserveService.cancelByCard("", 1L));
        assertThrows(ServiceException.class, () -> bookReserveService.cancelByCard("JS12345678", null));
    }
}
