package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.BorrowRecord;

/**
 * 借阅记录Service接口
 */
public interface IBorrowRecordService
{
    public BorrowRecord selectBorrowRecordByBorrowId(Long borrowId);

    public List<BorrowRecord> selectBorrowRecordList(BorrowRecord borrowRecord);

    public int insertBorrowRecord(BorrowRecord borrowRecord);

    public int updateBorrowRecord(BorrowRecord borrowRecord);

    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds);

    public int deleteBorrowRecordByBorrowId(Long borrowId);

    /** 还书：置归还日期并恢复图书库存 */
    public int returnBook(Long borrowId);

    /** 续借：应还日期 +30 天（逾期不可续借） */
    public int renewBook(Long borrowId);

    /** 前台借书：按借书证号（匿名接口） */
    public int borrowByCard(String cardNo, Long bookId);

    /** 前台续借：证号归属校验 + 未逾期 + 应还日期 +30 天 */
    public int renewByCard(String cardNo, Long borrowId);

    /** 按借书证号查询借阅记录 */
    public List<BorrowRecord> selectBorrowListByCard(String cardNo);

    /** 热门图书统计 */
    public java.util.List<java.util.Map<String, Object>> selectTopBooks();

    /** 读者借阅排行 */
    public java.util.List<java.util.Map<String, Object>> selectTopReaders();
}
