package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.service.BorrowRuleService;
import com.ruoyi.system.service.FineService;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.StatisticsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 借阅记录单元测试：删除还原库存/欠费拒绝 + 修改生命周期字段守卫（防绕过还书流程）
 */
@ExtendWith(MockitoExtension.class)
public class BorrowRecordServiceImplTest
{
    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ReaderMapper readerMapper;

    @Mock
    private com.ruoyi.system.mapper.SysNoticeMapper noticeMapper;

    @Mock
    private com.ruoyi.system.mapper.BookReserveMapper bookReserveMapper;

    @Mock
    private FineService fineService;

    @Mock
    private BorrowRuleService borrowRuleService;

    @Mock
    private IReaderService readerService;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private BorrowRecordServiceImpl borrowRecordService;

    /** 删除：未还记录(0) → 还原库存 + 删除 + 失效缓存 */
    @Test
    void deleteBorrowRecordByBorrowIds_unreturned_restoresStock()
    {
        BorrowRecord br = new BorrowRecord();
        br.setBorrowId(1L);
        br.setStatus("0");
        br.setBookId(3L);
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(br);
        when(borrowRecordMapper.deleteBorrowRecordByBorrowIds(any())).thenReturn(1);

        assertEquals(1, borrowRecordService.deleteBorrowRecordByBorrowIds(new Long[] { 1L }));
        verify(bookMapper).restoreStock(3L, 1L);
        verify(statisticsService).evictAll();
    }

    /** 删除：已归还记录(1) → 直接删，不还原库存 */
    @Test
    void deleteBorrowRecordByBorrowIds_returned_noRestore()
    {
        BorrowRecord br = new BorrowRecord();
        br.setBorrowId(1L);
        br.setStatus("1");
        br.setBookId(3L);
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(br);
        when(borrowRecordMapper.deleteBorrowRecordByBorrowIds(any())).thenReturn(1);

        assertEquals(1, borrowRecordService.deleteBorrowRecordByBorrowIds(new Long[] { 1L }));
        verify(bookMapper, never()).restoreStock(any(), any());
    }

    /** 删除：未还且未缴罚款（逾期+欠费） → 拒绝（删除会抹掉欠费） */
    @Test
    void deleteBorrowRecordByBorrowIds_unpaidFine_throws()
    {
        BorrowRecord br = new BorrowRecord();
        br.setBorrowId(1L);
        br.setStatus("2");
        br.setBookId(3L);
        br.setFineAmount(new BigDecimal("1.20"));
        br.setFinePaid("0");
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(br);

        ServiceException e = assertThrows(ServiceException.class,
                () -> borrowRecordService.deleteBorrowRecordByBorrowIds(new Long[] { 1L }));
        assertTrue(e.getMessage().contains("未缴罚款"));
        verify(bookMapper, never()).restoreStock(any(), any());
    }

    /** 修改：直改状态(0→1) → 拒绝（防绕过还书流程导致库存不还原） */
    @Test
    void updateBorrowRecord_statusChange_throws()
    {
        BorrowRecord old = new BorrowRecord();
        old.setBorrowId(1L);
        old.setStatus("0");
        BorrowRecord update = new BorrowRecord();
        update.setBorrowId(1L);
        update.setStatus("1");
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(old);

        ServiceException e = assertThrows(ServiceException.class,
                () -> borrowRecordService.updateBorrowRecord(update));
        assertTrue(e.getMessage().contains("还书"));
    }

    /** 修改：状态未变（仅改备注）→ 正常更新 */
    @Test
    void updateBorrowRecord_sameStatus_ok()
    {
        BorrowRecord old = new BorrowRecord();
        old.setBorrowId(1L);
        old.setStatus("0");
        BorrowRecord update = new BorrowRecord();
        update.setBorrowId(1L);
        update.setStatus("0");
        update.setRemark("改备注");
        when(borrowRecordMapper.selectBorrowRecordByBorrowId(1L)).thenReturn(old);
        when(borrowRecordMapper.updateBorrowRecord(any(BorrowRecord.class))).thenReturn(1);

        assertEquals(1, borrowRecordService.updateBorrowRecord(update));
    }
}
