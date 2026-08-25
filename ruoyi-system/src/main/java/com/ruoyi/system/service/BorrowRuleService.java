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
 * 报名规则服务：上限/借期/重复报名/续期次数（按成员类型差异化，参数可配）
 */
@Service
public class BorrowRuleService
{
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private ConfigUtil configUtil;

    /** 报名上限（按成员类型，参数 book.borrow.maxCount.{student|teacher|normal}） */
    public int maxCountFor(String readerType)
    {
        return configUtil.getTypeInt("book.borrow.maxCount", readerType, 5);
    }

    /** 借期天数（按成员类型，参数 book.borrow.days.{student|teacher|normal}） */
    public int daysFor(String readerType)
    {
        return configUtil.getTypeInt("book.borrow.days", readerType, 30);
    }

    /** 重复报名校验：同一本书未还不可再借 */
    public void checkNotBorrowing(Long readerId, Long bookId)
    {
        List<BorrowRecord> exists = borrowRecordMapper.selectBorrowingByReaderAndBook(readerId, bookId);
        if (exists != null && !exists.isEmpty())
        {
            throw new ServiceException("该成员已报名本服务且未完成，请先完成");
        }
    }

    /** 报名上限校验 */
    public void checkUnderLimit(Long readerId, int maxCount)
    {
        int borrowing = borrowRecordMapper.selectBorrowingCount(readerId);
        if (borrowing >= maxCount)
        {
            throw new ServiceException("报名数量已达上限（" + maxCount + " 个），请先完成部分服务");
        }
    }

    /**
     * 续期次数校验：超限抛异常，返回当前已续期次数
     */
    public long checkRenewAllowed(BorrowRecord record)
    {
        long renewLimit = configUtil.getInt("book.borrow.renewLimit", 1);
        long renewCount = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (renewCount >= renewLimit)
        {
            throw new ServiceException("该服务已续期过 " + renewCount + " 次，不可再次续期");
        }
        return renewCount;
    }

    /** 进行中/逾期的记录属于"未还" */
    public static boolean isBorrowing(BorrowRecord record)
    {
        return BizStatus.BORROW_OUT.equals(record.getStatus()) || BizStatus.BORROW_OVERDUE.equals(record.getStatus());
    }
}
