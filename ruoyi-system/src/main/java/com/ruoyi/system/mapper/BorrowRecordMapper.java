package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.BorrowRecord;

/**
 * 借阅记录Mapper接口
 */
public interface BorrowRecordMapper
{
    /** 查询借阅记录 */
    public BorrowRecord selectBorrowRecordByBorrowId(Long borrowId);

    /** 查询借阅记录列表 */
    public List<BorrowRecord> selectBorrowRecordList(BorrowRecord borrowRecord);

    /** 新增借阅记录 */
    public int insertBorrowRecord(BorrowRecord borrowRecord);

    /** 修改借阅记录 */
    public int updateBorrowRecord(BorrowRecord borrowRecord);

    /** 删除借阅记录 */
    public int deleteBorrowRecordByBorrowId(Long borrowId);

    /** 批量删除借阅记录 */
    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds);
}
