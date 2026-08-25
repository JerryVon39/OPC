package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.BorrowRecord;

/**
 * 报名记录Service接口
 */
public interface IBorrowRecordService
{
    public BorrowRecord selectBorrowRecordByBorrowId(Long borrowId);

    public List<BorrowRecord> selectBorrowRecordList(BorrowRecord borrowRecord);

    public int insertBorrowRecord(BorrowRecord borrowRecord);

    public int updateBorrowRecord(BorrowRecord borrowRecord);

    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds);

    public int deleteBorrowRecordByBorrowId(Long borrowId);

    /** 完成：置完成日期并恢复服务库存 */
    public int returnBook(Long borrowId);

    /** 罚款收款：标记已缴（收银台操作） */
    public int payFine(Long borrowId);

    /** 续期：截止日期 +30 天（逾期不可续期） */
    public int renewBook(Long borrowId);

    /** 前台报名：按成员编号（匿名接口） */
    public int borrowByCard(String cardNo, Long bookId);

    /** 前台续期：证号归属校验 + 未逾期 + 截止日期 +30 天 */
    public int renewByCard(String cardNo, Long borrowId);

    /** 按成员编号查询报名记录 */
    public List<BorrowRecord> selectBorrowListByCard(String cardNo);

    /** 热门服务统计 */
    public java.util.List<java.util.Map<String, Object>> selectTopBooks();

    /** 成员报名排行 */
    public java.util.List<java.util.Map<String, Object>> selectTopReaders();
}
