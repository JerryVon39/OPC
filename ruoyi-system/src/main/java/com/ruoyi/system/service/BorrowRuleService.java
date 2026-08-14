package com.ruoyi.system.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.constant.BizStatus;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.util.ConfigUtil;

/**
 * 借阅规则服务：上限/借期/重复借阅/续借次数（按读者类型差异化，参数可配）
 */
@Service
public class BorrowRuleService
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private ConfigUtil configUtil;

    /** 借阅上限（按读者类型，参数 book.borrow.maxCount.{student|teacher|normal}） */
    public int maxCountFor(String readerType)
    {
        return configUtil.getTypeInt("book.borrow.maxCount", readerType, 5);
    }

    /** 借期天数（按读者类型，参数 book.borrow.days.{student|teacher|normal}） */
    public int daysFor(String readerType)
    {
        return configUtil.getTypeInt("book.borrow.days", readerType, 30);
    }

    /** 重复借阅校验：同一本书未还不可再借 */
    public void checkNotBorrowing(Long readerId, Long bookId)
    {
        List<BorrowRecord> exists = borrowRecordMapper.selectBorrowingByReaderAndBook(readerId, bookId);
        if (exists != null && !exists.isEmpty())
        {
            throw new ServiceException("该读者已借阅本书且未归还，请先还书");
        }
    }

    /** 借阅上限校验 */
    public void checkUnderLimit(Long readerId, int maxCount)
    {
        int borrowing = borrowRecordMapper.selectBorrowingCount(readerId);
        if (borrowing >= maxCount)
        {
            throw new ServiceException("借阅数量已达上限（" + maxCount + " 本），请先归还部分图书");
        }
    }

    /**
     * 续借次数校验：超限抛异常，返回当前已续借次数
     */
    public long checkRenewAllowed(BorrowRecord record)
    {
        long renewLimit = configUtil.getInt("book.borrow.renewLimit", 1);
        long renewCount = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (renewCount >= renewLimit)
        {
            throw new ServiceException("该图书已续借过 " + renewCount + " 次，不可再次续借");
        }
        return renewCount;
    }

    /** 借出中/逾期的记录属于"未还" */
    public static boolean isBorrowing(BorrowRecord record)
    {
        return BizStatus.BORROW_OUT.equals(record.getStatus()) || BizStatus.BORROW_OVERDUE.equals(record.getStatus());
    }
}
