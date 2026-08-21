package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.BorrowRecord;

/**
 * 报名记录Mapper接口
 */
public interface BorrowRecordMapper
{
    /** 查询报名记录 */
    public BorrowRecord selectBorrowRecordByBorrowId(Long borrowId);

    /** 加锁查询报名记录（FOR UPDATE）：续借的校验与写入原子化，并发续借只有一次生效 */
    public BorrowRecord selectBorrowRecordByBorrowIdForUpdate(Long borrowId);

    /** 查询报名记录列表 */
    public List<BorrowRecord> selectBorrowRecordList(BorrowRecord borrowRecord);

    /** 新增报名记录 */
    public int insertBorrowRecord(BorrowRecord borrowRecord);

    /** 修改报名记录 */
    public int updateBorrowRecord(BorrowRecord borrowRecord);

    /** 仅当记录仍为"进行中"时标记逾期（定时任务用，防覆盖完成期间已还的记录） */
    public int markOverdue(@Param("borrowId") Long borrowId, @Param("updateTime") java.util.Date updateTime);

    /** 原子变更报名状态，返回实际更新行数 */
    public int updateStatusIfCurrent(@Param("borrowId") Long borrowId, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("returnDate") java.util.Date returnDate,
            @Param("fineAmount") java.math.BigDecimal fineAmount, @Param("finePaid") String finePaid,
            @Param("updateTime") java.util.Date updateTime);

    /** 删除报名记录 */
    public int deleteBorrowRecordByBorrowId(Long borrowId);

    /** 批量删除报名记录 */
    public int deleteBorrowRecordByBorrowIds(Long[] borrowIds);

    /** 按成员编号查询报名记录（前台"我的报名"用） */
    public List<BorrowRecord> selectBorrowListByCard(String cardNo);

    /** 统计成员未完成的报名数量 */
    public int selectBorrowingCount(Long readerId);

    /** 查询成员是否已借某本未还的书（防重复报名） */
    public List<BorrowRecord> selectBorrowingByReaderAndBook(Long readerId, Long bookId);

    /** 热门服务统计（按报名次数） */
    public java.util.List<java.util.Map<String, Object>> selectTopBooks(BorrowRecord borrowRecord);

    /** 首页数据看板：服务/成员/报名/订单聚合统计 */
    public java.util.Map<String, Object> selectDashboard();

    /** 补办换证号：同步该成员历史报名的快照证号 */
    public int updateCardNoSnapshot(@Param("readerId") Long readerId, @Param("newCardNo") String newCardNo);

    /** 查询当前真实逾期记录：状态"2"只由每日定时任务落库，未跑前逾期记录仍是"0"，
     * 必须按"进行中 + 截止日期已过"兜底（与 BorrowTask.remindOverdue 口径一致） */
    public java.util.List<BorrowRecord> selectOverdueRecords();

    /** 成员报名排行（按报名次数） */
    public java.util.List<java.util.Map<String, Object>> selectTopReaders(BorrowRecord borrowRecord);
}
