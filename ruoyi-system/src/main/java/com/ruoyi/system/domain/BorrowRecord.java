package com.ruoyi.system.domain;

import java.util.Date;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 借阅记录对象 borrow_record
 * 
 * @author ruoyi
 */
public class BorrowRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 借阅ID */
    private Long borrowId;

    /** 读者ID */
    @Excel(name = "读者ID")
    private Long readerId;

    /** 图书ID */
    @Excel(name = "图书ID")
    private Long bookId;

    /** 借出日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "借出日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date borrowDate;

    /** 应还日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "应还日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dueDate;

    /** 归还日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "归还日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date returnDate;

    /** 状态(0借出中 1已归还 2已逾期) */
    @Excel(name = "状态", readConverterExp = "0=借出中,1=已归还,2=已逾期")
    private String status;

    /** 备注 */
    private String remark;

    /** 联表查询：读者姓名 */
    @Excel(name = "读者姓名")
    private String readerName;

    /** 联表查询：借书证号 */
    @Excel(name = "借书证号")
    private String cardNo;

    /** 联表查询：图书名称 */
    @Excel(name = "图书名称")
    private String bookName;

    /** 逾期罚款金额(元) */
    @Excel(name = "罚款金额")
    private BigDecimal fineAmount;

    /** 罚款是否已缴(0未缴 1已缴) */
    @Excel(name = "罚款状态", readConverterExp = "0=未缴,1=已缴")
    private String finePaid;

    /** 已续借次数 */
    @Excel(name = "续借次数")
    private Long renewCount;

    public Long getBorrowId() { return borrowId; }
    public void setBorrowId(Long borrowId) { this.borrowId = borrowId; }

    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getReaderName() { return readerName; }
    public void setReaderName(String readerName) { this.readerName = readerName; }

    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public String getFinePaid() { return finePaid; }
    public void setFinePaid(String finePaid) { this.finePaid = finePaid; }

    public Long getRenewCount() { return renewCount; }
    public void setRenewCount(Long renewCount) { this.renewCount = renewCount; }
}
