package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 服务入驻申请申请对象 book_purchase_req
 *
 * 前台搜索无结果时可提交"我想看的书"，后台处理（待处理/已处理/已拒绝）
 *
 * @author ruoyi
 * @date 2026-08-18
 */
public class BookPurchaseReq extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long reqId;

    /** 项目/组织名称 */
    @Excel(name = "项目/组织名称")
    private String bookName;

    /** 联系人 */
    @Excel(name = "联系人")
    private String author;

    /** 申请者邮箱（入驻申请结果通知用，前台提交时填写） */
    private String email;

    /** 状态(0待处理 1已处理 2已拒绝) */
    @Excel(name = "状态(0待处理 1已处理 2已拒绝)")
    private String status;

    public void setReqId(Long reqId)
    {
        this.reqId = reqId;
    }

    public Long getReqId()
    {
        return reqId;
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

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
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
            .append("reqId", getReqId())
            .append("bookName", getBookName())
            .append("author", getAuthor())
            .append("email", getEmail())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}