package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /** 原子变更借阅状态，返回实际更新行数 */
    public int updateStatusIfCurrent(@Param("borrowId") Long borrowId, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("returnDate") java.util.Date returnDate,
            @Param("fineAmount") java.math.BigDecimal fineAmount, @Param("finePaid") String finePaid,
            @Param("updateTime") java.util.Date updateTime);

    /** 删除借阅记录 */
    public int deleteBorrowRecordByBorrowId(Long borrowId);

    /** 批量删除借阅记录 */
    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds);

    /** 按借书证号查询借阅记录（前台"我的借阅"用） */
    public List<BorrowRecord> selectBorrowListByCard(String cardNo);

    /** 统计读者未归还的借阅数量 */
    public int selectBorrowingCount(Long readerId);

    /** 查询读者是否已借某本未还的书（防重复借阅） */
    public List<BorrowRecord> selectBorrowingByReaderAndBook(Long readerId, Long bookId);

    /** 热门图书统计（按借阅次数） */
    public java.util.List<java.util.Map<String, Object>> selectTopBooks(BorrowRecord borrowRecord);

    /** 首页数据看板：图书/读者/借阅/订单聚合统计 */
    public java.util.Map<String, Object> selectDashboard();

    /** 补办换证号：同步该读者历史借阅的快照证号 */
    public int updateCardNoSnapshot(@Param("readerId") Long readerId, @Param("newCardNo") String newCardNo);

    /** 查询当前真实逾期记录：状态"2"只由每日定时任务落库，未跑前逾期记录仍是"0"，
     * 必须按"借出中 + 应还日期已过"兜底（与 BorrowTask.remindOverdue 口径一致） */
    public java.util.List<BorrowRecord> selectOverdueRecords();

    /** 读者借阅排行（按借阅次数） */
    public java.util.List<java.util.Map<String, Object>> selectTopReaders(BorrowRecord borrowRecord);
}
