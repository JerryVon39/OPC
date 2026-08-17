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
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.service.StatisticsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 读者单元测试：证号生成格式、证号查重、按证号查读者
 */
@ExtendWith(MockitoExtension.class)
public class ReaderServiceImplTest
{
    @Mock
    private ReaderMapper readerMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private ShopOrderMapper shopOrderMapper;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private ReaderServiceImpl readerService;

    private Reader reader;

    @BeforeEach
    void setUp()
    {
        reader = new Reader();
        reader.setReaderName("测试读者");
        reader.setPhone("13800000000");
        reader.setReaderType("1");
        reader.setStatus("0");
    }

    /** 登记：证号自动生成，格式 JS + 8 位数字 */
    @Test
    void register_generatesCardNo()
    {
        when(readerMapper.selectReaderList(any())).thenReturn(new ArrayList<>());
        when(readerMapper.insertReader(any(Reader.class))).thenReturn(1);

        Reader saved = readerService.register("测试读者", "13800000000", "1", "");
        assertNotNull(saved.getCardNo());
        assertTrue(saved.getCardNo().matches("JS\\d{8}"), "证号格式应为JS+8位数字，实际: " + saved.getCardNo());
        assertEquals("0", saved.getStatus());
    }

    /** 登记：证号重复 → 抛异常 */
    @Test
    void insertReader_duplicateCardNo_throws()
    {
        Reader exist = new Reader();
        exist.setCardNo("JS12345678");
        List<Reader> list = new ArrayList<>();
        list.add(exist);
        when(readerMapper.selectReaderList(any())).thenReturn(list);

        reader.setCardNo("JS12345678");
        ServiceException e = assertThrows(ServiceException.class, () -> readerService.insertReader(reader));
        assertTrue(e.getMessage().contains("已被使用"));
    }

    /** 添加读者：证号留空 → 自动生成 */
    @Test
    void insertReader_emptyCardNo_generates()
    {
        when(readerMapper.selectReaderList(any())).thenReturn(new ArrayList<>());
        when(readerMapper.insertReader(any(Reader.class))).thenReturn(1);

        assertEquals(1, readerService.insertReader(reader));
        assertTrue(reader.getCardNo().matches("JS\\d{8}"));
        assertNotNull(reader.getCreateTime());
        verify(statisticsService).evictAll();
    }

    /** 按证号查询：不存在 → 抛异常 */
    @Test
    void findActiveReader_notFound_throws()
    {
        when(readerMapper.selectReaderList(any())).thenReturn(new ArrayList<>());
        ServiceException e = assertThrows(ServiceException.class, () -> readerService.findActiveReader("JS00000000"));
        assertTrue(e.getMessage().contains("不存在"));
    }

    /** 按证号查询：存在 → 返回第一条 */
    @Test
    void findActiveReader_found()
    {
        reader.setCardNo("JS12345678");
        List<Reader> list = new ArrayList<>();
        list.add(reader);
        when(readerMapper.selectReaderList(any())).thenReturn(list);

        assertEquals(reader, readerService.findActiveReader("JS12345678"));
    }

    /** 修改读者：证号与他人撞 → 抛（防撞 uk_card_no 唯一约束变裸数据库异常） */
    @Test
    void updateReader_duplicateCardNo_throws()
    {
        Reader other = new Reader();
        other.setReaderId(99L);
        other.setCardNo("JS12345678");
        List<Reader> list = new ArrayList<>();
        list.add(other);
        when(readerMapper.selectReaderList(any())).thenReturn(list);

        reader.setReaderId(1L);
        reader.setCardNo("JS12345678");
        ServiceException e = assertThrows(ServiceException.class, () -> readerService.updateReader(reader));
        assertTrue(e.getMessage().contains("已被使用"));
    }

    /** 修改读者：证号未变（查重命中自己）→ 正常更新 */
    @Test
    void updateReader_sameCardNo_ok()
    {
        Reader self = new Reader();
        self.setReaderId(1L);
        self.setCardNo("JS12345678");
        List<Reader> list = new ArrayList<>();
        list.add(self);
        when(readerMapper.selectReaderList(any())).thenReturn(list);
        when(readerMapper.updateReader(any(Reader.class))).thenReturn(1);

        reader.setReaderId(1L);
        reader.setCardNo("JS12345678");
        assertEquals(1, readerService.updateReader(reader));
        verify(readerMapper).updateReader(any(Reader.class));
    }

    /** 修改读者：不填证号 → 不查重直接更新 */
    @Test
    void updateReader_noCardNo_ok()
    {
        when(readerMapper.updateReader(any(Reader.class))).thenReturn(1);
        reader.setReaderId(1L);
        assertEquals(1, readerService.updateReader(reader));
        verify(readerMapper, never()).selectReaderList(any());
    }
}
