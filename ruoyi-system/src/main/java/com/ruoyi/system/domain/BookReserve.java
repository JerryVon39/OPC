package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 服务候补对象 book_reserve
 */
public class BookReserve extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 候补ID */
    private Long reserveId;

    /** 服务ID */
    private Long bookId;

    /** 成员ID */
    private Long readerId;

    /** 成员姓名(快照) */
    private String readerName;

    /** 成员编号(快照) */
    private String cardNo;

    /** 服务名称(快照) */
    private String bookName;

    /** 候补时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reserveDate;

    /** 状态(0候补中 1有名额 2已完成 3已取消) */
    private String status;

    public void setReserveId(Long reserveId) { this.reserveId = reserveId; }
    public Long getReserveId() { return reserveId; }

    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Long getBookId() { return bookId; }

    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public Long getReaderId() { return readerId; }

    public void setReaderName(String readerName) { this.readerName = readerName; }
    public String getReaderName() { return readerName; }

    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public String getCardNo() { return cardNo; }

    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getBookName() { return bookName; }

    public void setReserveDate(Date reserveDate) { this.reserveDate = reserveDate; }
    public Date getReserveDate() { return reserveDate; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reserveId", getReserveId())
            .append("bookId", getBookId())
            .append("readerId", getReaderId())
            .append("readerName", getReaderName())
            .append("cardNo", getCardNo())
            .append("bookName", getBookName())
            .append("reserveDate", getReserveDate())
            .append("status", getStatus())
            .toString();
    }
}
