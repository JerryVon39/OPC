package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.constant.BizStatus;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.util.ConfigUtil;

/**
 * 逾期罚款服务：计算/收款/欠费检查
 */
@Service
public class FineService
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private ConfigUtil configUtil;

    /**
     * 计算逾期罚款：逾期超过免罚天数按天计罚（参数 book.fine.perDay / book.fine.graceDays）
     * 无罚款返回 null
     */
    public BigDecimal calcFine(BorrowRecord record)
    {
        if (record.getDueDate() == null || !record.getDueDate().before(new Date()))
        {
            return null;
        }
        int grace = configUtil.getInt("book.fine.graceDays", 0);
        long overdueDays = (new Date().getTime() - record.getDueDate().getTime()) / (24L * 3600 * 1000);
        if (overdueDays <= grace)
        {
            return null;
        }
        BigDecimal perDay = BigDecimal.valueOf(configUtil.getDouble("book.fine.perDay", 0.1));
        BigDecimal fine = perDay.multiply(BigDecimal.valueOf(overdueDays - grace));
        return fine.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /** 欠费检查（借书前）：有未缴罚款则抛异常 */
    public void checkNoUnpaidFine(Long readerId)
    {
        BorrowRecord q = new BorrowRecord();
        q.setReaderId(readerId);
        q.setFinePaid(BizStatus.FINE_UNPAID);
        List<BorrowRecord> unpaid = borrowRecordMapper.selectBorrowRecordList(q);
        for (BorrowRecord fr : unpaid)
        {
            if (fr.getFineAmount() != null && fr.getFineAmount().compareTo(BigDecimal.ZERO) > 0)
            {
                throw new ServiceException("该读者有未缴罚款（" + fr.getFineAmount() + " 元），请先到服务台缴费");
            }
        }
    }

    /** 罚款收款：标记已缴（收银台操作） */
    @Transactional
    public int payFine(Long borrowId)
    {
        BorrowRecord record = borrowRecordMapper.selectBorrowRecordByBorrowId(borrowId);
        if (record == null)
        {
            throw new ServiceException("借阅记录不存在");
        }
        if (record.getFineAmount() == null || record.getFineAmount().compareTo(BigDecimal.ZERO) <= 0
                || BizStatus.FINE_PAID.equals(record.getFinePaid()))
        {
            throw new ServiceException("该记录无待缴罚款");
        }
        record.setFinePaid(BizStatus.FINE_PAID);
        record.setUpdateTime(new Date());
        return borrowRecordMapper.updateBorrowRecord(record);
    }
}
