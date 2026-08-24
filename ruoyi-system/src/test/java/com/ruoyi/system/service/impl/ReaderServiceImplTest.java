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
        when(readerMapper.insertReader(any(Reader.class))).thenReturn(1);

        Reader saved = readerService.register("测试读者", "13800000000", "1", "test@qq.com", "");
        assertNotNull(saved.getCardNo());
        assertTrue(saved.getCardNo().matches("JS\\d{8}"), "证号格式应为JS+8位数字，实际: " + saved.getCardNo());
        assertEquals("0", saved.getStatus());
        assertEquals("test@qq.com", saved.getEmail());
    }

    /** 登记：证号重复 → 抛异常 */
    @Test
    void insertReader_duplicateCardNo_throws()
    {
        when(readerMapper.countByCardNo("JS12345678")).thenReturn(1);

        reader.setCardNo("JS12345678");
        ServiceException e = assertThrows(ServiceException.class, () -> readerService.insertReader(reader));
        assertTrue(e.getMessage().contains("已被使用"));
    }

    /** 添加读者：证号留空 → 自动生成 */
    @Test
    void insertReader_emptyCardNo_generates()
    {
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

    // ===== 批量导入 =====

    @Mock
    private com.ruoyi.system.service.ISysDictDataService sysDictDataService;

    private java.util.List<com.ruoyi.common.core.domain.entity.SysDictData> readerTypeDict()
    {
        java.util.List<com.ruoyi.common.core.domain.entity.SysDictData> list = new ArrayList<>();
        for (String v : new String[] { "1", "2", "3" })
        {
            com.ruoyi.common.core.domain.entity.SysDictData d = new com.ruoyi.common.core.domain.entity.SysDictData();
            d.setDictValue(v);
            list.add(d);
        }
        return list;
    }

    private Reader reader(String name, String phone, String type, String cardNo)
    {
        Reader r = new Reader();
        r.setReaderName(name);
        r.setPhone(phone);
        r.setEmail("t" + (name == null ? "" : name.hashCode()) + "@qq.com");
        r.setReaderType(type);
        r.setCardNo(cardNo);
        return r;
    }

    /** 导入成功：证号留空 → 自动生成（走 insertReader），success=1 */
    @Test
    void importReaders_success_cardNoGenerated()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(readerTypeDict());
        when(readerMapper.insertReader(any(Reader.class))).thenReturn(1);

        List<Reader> list = new ArrayList<>();
        list.add(reader("张三", "13800000001", "1", null));
        java.util.Map<String, Object> r = readerService.importReaders(list);

        assertEquals(1, r.get("success"));
        assertEquals(0, r.get("fail"));
        verify(readerMapper).insertReader(any(Reader.class));
    }

    /** 导入判重：证号已存在 → 跳过并提示 */
    @Test
    void importReaders_duplicateCardNo_skipped()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(readerTypeDict());
        when(readerMapper.countByCardNo("JS12345678")).thenReturn(1);

        List<Reader> list = new ArrayList<>();
        list.add(reader("张三", "13800000001", "1", "JS12345678"));
        java.util.Map<String, Object> r = readerService.importReaders(list);

        assertEquals(0, r.get("success"));
        assertEquals(1, r.get("fail"));
        assertTrue(r.get("errors").toString().contains("已存在"));
        verify(readerMapper, never()).insertReader(any(Reader.class));
    }

    /** 导入校验：手机号格式不对 → 跳过并提示 */
    @Test
    void importReaders_invalidPhone_skipped()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(readerTypeDict());

        List<Reader> list = new ArrayList<>();
        list.add(reader("张三", "123", "1", null));
        java.util.Map<String, Object> r = readerService.importReaders(list);

        assertEquals(0, r.get("success"));
        assertTrue(r.get("errors").toString().contains("手机号格式不正确"));
        verify(readerMapper, never()).insertReader(any(Reader.class));
    }

    /** 导入混合：1 成功 + 1 证号重复 + 1 手机号错 */
    @Test
    void importReaders_mixed()
    {
        when(sysDictDataService.selectDictDataList(any())).thenReturn(readerTypeDict());
        when(readerMapper.countByCardNo(anyString())).thenReturn(0);   // generateCardNo 随机证号
        when(readerMapper.countByCardNo("JS12345678")).thenReturn(1);
        when(readerMapper.insertReader(any(Reader.class))).thenReturn(1);

        List<Reader> list = new ArrayList<>();
        list.add(reader("张三", "13800000001", "1", null));       // 成功
        list.add(reader("李四", "13800000002", "2", "JS12345678")); // 证号重复
        list.add(reader("王五", "abc", "3", null));                 // 手机号错
        java.util.Map<String, Object> r = readerService.importReaders(list);

        assertEquals(1, r.get("success"));
        assertEquals(2, r.get("fail"));
        assertEquals(2, ((java.util.List<?>) r.get("errors")).size());
    }
}
