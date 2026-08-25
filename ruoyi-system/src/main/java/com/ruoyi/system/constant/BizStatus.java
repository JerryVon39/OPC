package com.ruoyi.system.constant;

/**
 * 业务状态常量（消灭魔法字符串）
 */
public class BizStatus
{
    /** 服务招募中 */
    public static final String BOOK_ON_SALE = "0";
    /** 服务已结束 */
    public static final String BOOK_OFF_SALE = "1";

    /** 成员正常 */
    public static final String READER_NORMAL = "0";
    /** 成员停用/挂失 */
    public static final String READER_DISABLED = "1";

    /** 借出中 */
    public static final String BORROW_OUT = "0";
    /** 已归还 */
    public static final String BORROW_RETURNED = "1";
    /** 已逾期 */
    public static final String BORROW_OVERDUE = "2";

    /** 订单待付款 */
    public static final String ORDER_UNPAID = "0";
    /** 订单已完成 */
    public static final String ORDER_COMPLETED = "1";
    /** 订单已取消 */
    public static final String ORDER_CANCELLED = "2";
    /** 订单已收款 */
    public static final String ORDER_PAID = "3";

    /** 预约中 */
    public static final String RESERVE_WAITING = "0";
    /** 预约可借 */
    public static final String RESERVE_READY = "1";
    /** 预约已完成 */
    public static final String RESERVE_DONE = "2";
    /** 预约已取消 */
    public static final String RESERVE_CANCELLED = "3";

    /** 罚款未缴 */
    public static final String FINE_UNPAID = "0";
    /** 罚款已缴 */
    public static final String FINE_PAID = "1";
}
