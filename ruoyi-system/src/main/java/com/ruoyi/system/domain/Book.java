package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 图书信息对象 book
 * 
 * @author ruoyi
 * @date 2026-08-12
 */
public class Book extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 图书ID */
    private Long bookId;

    /** 图书名称 */
    @Excel(name = "图书名称")
    private String bookName;

    /** 作者 */
    @Excel(name = "作者")
    private String author;

    /** 图书类型(字典:book_type) */
    @Excel(name = "图书类型(字典:book_type)")
    private String bookType;

    /** 出版社 */
    @Excel(name = "出版社")
    private String publisher;

    /** 价格(元) */
    @Excel(name = "价格(元)")
    private BigDecimal price;

    /** 出版日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出版日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date publishDate;

    /** 库存数量 */
    @Excel(name = "库存数量")
    private Long stock;

    /** 状态(0在架 1下架) */
    @Excel(name = "状态(0在架 1下架)")
    private String status;

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
