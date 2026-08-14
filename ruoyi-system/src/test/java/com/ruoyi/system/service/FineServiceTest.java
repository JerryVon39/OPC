package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 逾期罚款规则单元测试：计算（免罚天数/按天计费）、欠费检查、收款
 */
@ExtendWith(MockitoExtension.class)
public class FineServiceTest
{
    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private ConfigUtil configUtil;

    @InjectMocks
    private FineService fineService;

    private BorrowRecord record;

    @BeforeEach
    void setUp()
    {
        record = new BorrowRecord();
        record.setBorrowId(1L);
        record.setReaderId(2L);
    }

    /** 应还日期在未来：无罚款 */
    @Test
    void calcFine_notDue_returnsNull()
    {
        record.setDueDate(new Date(System.currentTimeMillis() + 24L * 3600 * 1000));
        assertNull(fineService.calcFine(record));
        verifyNoInteractions(configUtil);
    }

    /** 应还日期为空：无罚款 */
    @Test
    void calcFine_noDueDate_returnsNull()
    {
        assertNull(fineService.calcFine(record));
    }

    /** 逾期 2 天、免罚 2 天：在免罚期内，无罚款 */
    @Test
    void calcFine_overdueWithinGrace_returnsNull()
    {
        record.setDueDate(new Date(System.currentTimeMillis() - 2L * 24 * 3600 * 1000));
        when(configUtil.getInt("book.fine.graceDays", 0)).thenReturn(2);
        assertNull(fineService.calcFine(record));
    }

    /** 逾期 3 天、免罚 1 天、单价 0.10：罚 2 天 = 0.20 元 */
    @Test
    void calcFine_overdueThreeDays_chargeTwoDays()
    {
        record.setDueDate(new Date(System.currentTimeMillis() - 3L * 24 * 3600 * 1000));
        when(configUtil.getInt("book.fine.graceDays", 0)).thenReturn(1);
        when(configUtil.getDouble("book.fine.perDay", 0.1)).thenReturn(0.10);
        BigDecimal fine = fineService.calcFine(record);
        assertNotNull(fine);
        assertEquals(0, fine.compareTo(new BigDecimal("0.20")));
    }

    /** 无免罚天数、逾期 3 天、单价 0.10：罚 0.30 元 */
    @Test
    void calcFine_noGrace_chargeFullDays()
    {
        record.setDueDate(new Date(System.currentTimeMillis() - 3L * 24 * 3600 * 1000));
        when(configUtil.getInt("book.fine.graceDays", 0)).thenReturn(0);
        when(configUtil.getDouble("book.fine.perDay", 0.1)).thenReturn(0.10);
        BigDecimal fine = fineService.calcFine(record);
        assertNotNull(fine);
        assertEquals(0, fine.compareTo(new BigDecimal("0.30")));
    }

    /** 欠费检查：有未缴罚款 → 抛异常 */
    @Test
    void checkNoUnpaidFine_hasUnpaid_throws()
    {
        BorrowRecord unpaid = new BorrowRecord();
        unpaid.setFineAmount(new BigDecimal("0.50"));
        unpaid.setFinePaid("0");
        List<BorrowRecord> list = new ArrayList<>();
        list.add(unpaid);
        when(borrowRecordMapper.selectBorrowRecordList(any())).thenReturn(list);
        ServiceException e = assertThrows(ServiceException.class, () -> fineService.checkNoUnpaidFine(2L));
        assertTrue(e.getMessage().contains("未缴罚款"));
    }

    /** 欠费检查：罚款金额为 0 → 不拦截 */
    @Test
    void checkNoUnpaidFine_zeroAmount_ok()
    {
        BorrowRecord unpaid = new BorrowRecord();
        unpaid.setFineAmount(BigDecimal.ZERO);
        List<BorrowRecord> list = new ArrayList<>();
        list.add(unpaid);
        when(borrowRecordMapper.selectBorrowRecordList(any())).thenReturn(list);
        assertDoesNotThrow(() -> fineService.checkNoUnpaidFine(2L));
    }

    /** 欠费检查：无记录 → 通过 */
    @Test
    void checkNoUnpaidFine_noRecords_ok()
    {
        when(borrowRecordMapper.selectBorrowRecordList(any())).thenReturn(new ArrayList<>());
        assertDoesNotThrow(() -> fineService.checkNoUnpaidFine(2L));
    }

    /** 收款：记录不存在 → 抛异常 */
    @Test
    void payFine_notFound_throws()
    {
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> fineService.payFine(1L));
    }

    /** 收款：无罚款金额 → 抛"无待缴罚款" */
    @Test
    void payFine_noFineAmount_throws()
    {
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(record);
        assertThrows(ServiceException.class, () -> fineService.payFine(1L));
    }

    /** 收款：已缴 → 抛"无待缴罚款" */
    @Test
    void payFine_alreadyPaid_throws()
    {
        record.setFineAmount(new BigDecimal("0.50"));
        record.setFinePaid("1");
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(record);
        assertThrows(ServiceException.class, () -> fineService.payFine(1L));
    }

    /** 收款：正常流程 → 标记已缴并更新 */
    @Test
    void payFine_success_marksPaid()
    {
        record.setFineAmount(new BigDecimal("0.50"));
        record.setFinePaid("0");
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(record);
        when(borrowRecordMapper.updateBorrowRecord(record)).thenReturn(1);
        assertEquals(1, fineService.payFine(1L));
        assertEquals("1", record.getFinePaid());
        assertNotNull(record.getUpdateTime());
    }
}
