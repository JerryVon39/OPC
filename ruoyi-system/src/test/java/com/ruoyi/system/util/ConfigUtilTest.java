package com.ruoyi.system.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.system.service.ISysConfigService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 参数读取工具单元测试：默认值回退、类型转换、按读者类型取键
 */
@ExtendWith(MockitoExtension.class)
public class ConfigUtilTest
{
    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private ConfigUtil configUtil;

    /** 正常读取整数 */
    @Test
    void getInt_normal()
    {
        when(configService.selectConfigByKey("book.borrow.renewLimit")).thenReturn("2");
        assertEquals(2, configUtil.getInt("book.borrow.renewLimit", 1));
    }

    /** 键不存在 → 回退默认值 */
    @Test
    void getInt_missing_fallback()
    {
        when(configService.selectConfigByKey("book.none")).thenReturn(null);
        assertEquals(5, configUtil.getInt("book.none", 5));
    }

    /** 值非数字 → 回退默认值（不抛异常） */
    @Test
    void getInt_invalidValue_fallback()
    {
        when(configService.selectConfigByKey("book.bad")).thenReturn("abc");
        assertEquals(5, configUtil.getInt("book.bad", 5));
    }

    /** 正常读取小数 */
    @Test
    void getDouble_normal()
    {
        when(configService.selectConfigByKey("book.fine.perDay")).thenReturn("0.10");
        assertEquals(0.10, configUtil.getDouble("book.fine.perDay", 0.1), 0.0001);
    }

    /** 小数异常 → 回退默认值 */
    @Test
    void getDouble_invalid_fallback()
    {
        when(configService.selectConfigByKey("book.fine.perDay")).thenReturn("x");
        assertEquals(0.1, configUtil.getDouble("book.fine.perDay", 0.1), 0.0001);
    }

    /** 学生类型：类型参数存在 → 用类型参数 */
    @Test
    void getTypeInt_student_usesSuffix()
    {
        when(configService.selectConfigByKey("book.borrow.maxCount.student")).thenReturn("10");
        assertEquals(10, configUtil.getTypeInt("book.borrow.maxCount", "1", 5));
    }

    /** 教师类型：类型参数缺失 → 回退通用键 */
    @Test
    void getTypeInt_teacher_missingSuffix_fallbackToCommon()
    {
        when(configService.selectConfigByKey("book.borrow.days.teacher")).thenReturn(null);
        when(configService.selectConfigByKey("book.borrow.days")).thenReturn("30");
        assertEquals(30, configUtil.getTypeInt("book.borrow.days", "2", 30));
    }

    /** 未知类型 → 直接回退通用键 */
    @Test
    void getTypeInt_unknownType_fallbackToCommon()
    {
        when(configService.selectConfigByKey("book.borrow.maxCount")).thenReturn("5");
        assertEquals(5, configUtil.getTypeInt("book.borrow.maxCount", "9", 5));
    }
}
