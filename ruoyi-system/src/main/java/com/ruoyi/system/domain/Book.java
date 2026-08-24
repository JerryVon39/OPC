package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 服务信息对象 book
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
public class Book extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 服务ID */
    private Long bookId;

    /** 服务名称 */
    @Excel(name = "服务名称")
    private String bookName;

    /** 主办方 */
    @Excel(name = "主办方")
    private String author;

    /** 服务分类(字典:book_type) */
    @Excel(name = "服务分类(字典:book_type)")
    private String bookType;

    /** 合作机构 */
    @Excel(name = "合作机构")
    private String publisher;

    /** 费用(元) */
    @Excel(name = "费用(元)")
    private BigDecimal price;

    /** 上线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "上线时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date publishDate;

    /** 剩余名额 */
    @Excel(name = "剩余名额")
    private Long stock;

    /** 状态(0在架 1下架) */
    @Excel(name = "状态(0在架 1下架)")
    private String status;

    private String remark;

    /** 封面图片 */
    private String cover;

    /** ISBN */
    private String isbn;

    /** 服务介绍 */
    private String intro;

    /** 查询关键字（服务名称或主办方） */
    private String keyword;

    /** 报名次数（列表查询派生：报名记录统计字段，非数据库列） */
    private Long borrowCount;

    /** 删除标志（0存在 2删除，软删除两态） */
    private String delFlag;

    /** 回收站ID（回收站列表/还原用，非 book 表列） */
    private Long recycleId;

    /** 删除时间（回收站列表用，非 book 表列） */
    private Date deletedTime;

    /** 删除人（回收站列表用，非 book 表列） */
    private String deletedBy;

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Long getBorrowCount() { return borrowCount; }
    public void setBorrowCount(Long borrowCount) { this.borrowCount = borrowCount; }

    public Long getRecycleId() { return recycleId; }
    public void setRecycleId(Long recycleId) { this.recycleId = recycleId; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Date getDeletedTime() { return deletedTime; }
    public void setDeletedTime(Date deletedTime) { this.deletedTime = deletedTime; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public void setBookId(Long bookId) 
    {
        this.bookId = bookId;
    }

    public Long getBookId() 
    {
        return bookId;
    }

    public void setBookName(String bookName) 
    {
        this.bookName = bookName;
    }

    public String getBookName() 
    {
        return bookName;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    public String getAuthor() 
    {
        return author;
    }

    public void setBookType(String bookType) 
    {
        this.bookType = bookType;
    }

    public String getBookType() 
    {
        return bookType;
    }

    public void setPublisher(String publisher) 
    {
        this.publisher = publisher;
    }

    public String getPublisher() 
    {
        return publisher;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
    }

    public void setPublishDate(Date publishDate) 
    {
        this.publishDate = publishDate;
    }

    public Date getPublishDate() 
    {
        return publishDate;
    }

    public void setStock(Long stock) 
    {
        this.stock = stock;
    }

    public Long getStock() 
    {
        return stock;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("bookId", getBookId())
            .append("bookName", getBookName())
            .append("author", getAuthor())
            .append("bookType", getBookType())
            .append("publisher", getPublisher())
            .append("price", getPrice())
            .append("publishDate", getPublishDate())
            .append("stock", getStock())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
