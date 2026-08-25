package com.ruoyi.system.service;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.util.ConfigUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 借阅规则单元测试：按类型取上限/借期、重复借校验、上限校验、续借次数限制
 */
@ExtendWith(MockitoExtension.class)
public class BorrowRuleServiceTest
{
    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private ConfigUtil configUtil;

    @InjectMocks
    private BorrowRuleService borrowRuleService;

    private BorrowRecord record;

    @BeforeEach
    void setUp()
    {
        record = new BorrowRecord();
        record.setReaderId(2L);
        record.setBookId(3L);
    }

    /** 学生类型 → 取 student 参数 */
    @Test
    void maxCountFor_student_usesTypeParam()
    {
        when(configUtil.getTypeInt("book.borrow.maxCount", "1", 5)).thenReturn(10);
        assertEquals(10, borrowRuleService.maxCountFor("1"));
    }

    /** 教师类型 → 借期 60 天 */
    @Test
    void daysFor_teacher_usesTypeParam()
    {
        when(configUtil.getTypeInt("book.borrow.days", "2", 30)).thenReturn(60);
        assertEquals(60, borrowRuleService.daysFor("2"));
    }

    /** 重复借校验：有未还记录 → 抛异常 */
    @Test
    void checkNotBorrowing_hasRecord_throws()
    {
        List<BorrowRecord> list = new ArrayList<>();
        list.add(record);
        when(borrowRecordMapper.selectBorrowingByReaderAndBook(2L, 3L)).thenReturn(list);
        ServiceException e = assertThrows(ServiceException.class, () -> borrowRuleService.checkNotBorrowing(2L, 3L));
        assertTrue(e.getMessage().contains("已报名本服务"));
    }

    /** 重复借校验：无记录 → 通过 */
    @Test
    void checkNotBorrowing_noRecord_ok()
    {
        when(borrowRecordMapper.selectBorrowingByReaderAndBook(2L, 3L)).thenReturn(new ArrayList<>());
        assertDoesNotThrow(() -> borrowRuleService.checkNotBorrowing(2L, 3L));
    }

    /** 上限校验：已达上限 → 抛异常 */
    @Test
    void checkUnderLimit_atLimit_throws()
    {
        when(borrowRecordMapper.selectBorrowingCount(2L)).thenReturn(5);
        ServiceException e = assertThrows(ServiceException.class, () -> borrowRuleService.checkUnderLimit(2L, 5));
        assertTrue(e.getMessage().contains("已达上限"));
    }

    /** 上限校验：未到上限 → 通过 */
    @Test
    void checkUnderLimit_belowLimit_ok()
    {
        when(borrowRecordMapper.selectBorrowingCount(2L)).thenReturn(4);
        assertDoesNotThrow(() -> borrowRuleService.checkUnderLimit(2L, 5));
    }

    /** 续借：已续 1 次、上限 1 次 → 抛异常 */
    @Test
    void checkRenewAllowed_atLimit_throws()
    {
        record.setRenewCount(1L);
        when(configUtil.getInt("book.borrow.renewLimit", 1)).thenReturn(1);
        ServiceException e = assertThrows(ServiceException.class, () -> borrowRuleService.checkRenewAllowed(record));
        assertTrue(e.getMessage().contains("不可再次续期"));
    }

    /** 续借：未续借过 → 返回 0 */
    @Test
    void checkRenewAllowed_firstRenew_returnsZero()
    {
        when(configUtil.getInt("book.borrow.renewLimit", 1)).thenReturn(1);
        assertEquals(0L, borrowRuleService.checkRenewAllowed(record));
    }

    /** 续借：renewCount 为 null → 视为 0 次 */
    @Test
    void checkRenewAllowed_nullRenewCount_ok()
    {
        when(configUtil.getInt("book.borrow.renewLimit", 1)).thenReturn(1);
        assertEquals(0L, borrowRuleService.checkRenewAllowed(record));
    }

    /** isBorrowing 静态判断：借出中(0)/逾期(2) 视为未还，已归还(1) 不算 */
    @Test
    void isBorrowing_statusCheck()
    {
        record.setStatus("0");
        assertTrue(BorrowRuleService.isBorrowing(record));
        record.setStatus("2");
        assertTrue(BorrowRuleService.isBorrowing(record));
        record.setStatus("1");
        assertFalse(BorrowRuleService.isBorrowing(record));
    }
}
