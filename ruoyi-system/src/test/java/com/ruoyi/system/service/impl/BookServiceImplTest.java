package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BookReserveMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.service.ISysDictDataService;
import com.ruoyi.system.service.StatisticsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图书单元测试：批量导入（判重/字典校验/错误收集）
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest
{
    @Mock
    private BookMapper bookMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private ShopOrderMapper shopOrderMapper;

    @Mock
    private BookReserveMapper bookReserveMapper;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private ISysDictDataService sysDictDataService;

    @InjectMocks
    private BookServiceImpl bookService;

    private List<SysDictData> bookTypeDict()
    {
        List<SysDictData> list = new ArrayList<>();
        for (String v : new String[] { "1", "2", "3" })
        {
            SysDictData d = new SysDictData();
            d.setDictValue(v);
            list.add(d);
        }
        return list;
    }

    private Book book(String name, String type)
    {
        Book b = new Book();
        b.setBookName(name);
        b.setBookType(type);
        b.setStock(10L);
        b.setPrice(new java.math.BigDecimal("20.00"));
        return b;
    }

    @Test
    void importBooks_success_inserts()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(bookTypeDict());
        when(bookMapper.countByBookName("三体")).thenReturn(0);
        when(bookMapper.insertBook(any(Book.class))).thenReturn(1);

        List<Book> list = new ArrayList<>();
        list.add(book("三体", "1"));
        Map<String, Object> r = bookService.importBooks(list);

        assertEquals(1, r.get("success"));
        assertEquals(0, r.get("fail"));
        verify(bookMapper).insertBook(any(Book.class));
    }

    @Test
    void importBooks_duplicateName_skippedWithError()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(bookTypeDict());
        when(bookMapper.countByBookName("三体")).thenReturn(1);

        List<Book> list = new ArrayList<>();
        list.add(book("三体", "1"));
        Map<String, Object> r = bookService.importBooks(list);

        assertEquals(0, r.get("success"));
        assertEquals(1, r.get("fail"));
        assertTrue(r.get("errors").toString().contains("已存在"));
        verify(bookMapper, never()).insertBook(any(Book.class));
    }

    @Test
    void importBooks_invalidType_skippedWithError()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(bookTypeDict());

        List<Book> list = new ArrayList<>();
        list.add(book("星际穿越", "9")); // 类型 9 不在字典
        Map<String, Object> r = bookService.importBooks(list);

        assertEquals(0, r.get("success"));
        assertTrue(r.get("errors").toString().contains("不在字典内"));
        verify(bookMapper, never()).insertBook(any(Book.class));
    }

    @Test
    void importBooks_emptyName_skippedWithError()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(bookTypeDict());

        List<Book> list = new ArrayList<>();
        list.add(book("  ", "1"));
        Map<String, Object> r = bookService.importBooks(list);

        assertEquals(0, r.get("success"));
        assertTrue(r.get("errors").toString().contains("书名不能为空"));
    }

    @Test
    void importBooks_mixed_validAndInvalid()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(bookTypeDict());
        when(bookMapper.countByBookName("新书A")).thenReturn(0);
        when(bookMapper.countByBookName("已有书")).thenReturn(1);
        when(bookMapper.insertBook(any(Book.class))).thenReturn(1);

        List<Book> list = new ArrayList<>();
        list.add(book("新书A", "1"));   // 成功
        list.add(book("已有书", "2"));  // 重名跳过
        list.add(book("", "1"));        // 空名跳过
        Map<String, Object> r = bookService.importBooks(list);

        assertEquals(1, r.get("success"));
        assertEquals(2, r.get("fail"));
        assertEquals(2, ((List<?>) r.get("errors")).size());
    }
}