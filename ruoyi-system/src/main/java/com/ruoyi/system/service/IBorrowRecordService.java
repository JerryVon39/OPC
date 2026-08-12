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

    /** 按借书证号查询借阅记录 */
    public List<BorrowRecord> selectBorrowListByCard(String cardNo);

    /** 热门图书统计 */
    public java.util.List<java.util.Map<String, Object>> selectTopBooks();

    /** 读者借阅排行 */
    public java.util.List<java.util.Map<String, Object>> selectTopReaders();
}
