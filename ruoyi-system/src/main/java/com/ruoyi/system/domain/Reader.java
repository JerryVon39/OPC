package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员管理对象 reader
 * 
 * @author Jerry
 * @date 2026-08-12
 */
public class Reader extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long readerId;

    /** 成员姓名 */
    @Excel(name = "成员姓名")
    private String readerName;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phone;

    /** 电子邮箱（新成员登记必填，用于自动邮件通知） */
    @Excel(name = "电子邮箱")
    private String email;

    /** BCrypt 密码哈希（NULL=未设置密码；不导出/不回显） */
    private String passwordHash;

    /** 是否已设置密码(0未设置 1已设置) */
    private String pwdSet;

    /** 邮箱已验证(0未验证 1已验证) */
    private String emailVerified;

    /** 手机已验证(0未验证 1已验证，短信通道预留) */
    private String phoneVerified;

    /** 成员编号 */
    @Excel(name = "成员编号")
    private String cardNo;

    /** 成员类型 */
    @Excel(name = "成员类型")
    private String readerType;

    /** 性别(0男 1女 2未知) */
    @Excel(name = "性别(0男 1女 2未知)")
    private String sex;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthDate;

    /** 状态(0正常 1停用) */
    @Excel(name = "状态(0正常 1停用)")
    private String status;

    /** 最近登录时间（个人主页展示，不导出） */
    private java.util.Date lastLoginTime;

    /** 删除标志（0存在 2删除，软删除两态） */
    private String delFlag;

    /** 回收站ID（回收站列表/还原用，非 reader 表列） */
    private Long recycleId;

    /** 删除时间（回收站列表用，非 reader 表列） */
    private java.util.Date deletedTime;

    /** 删除人（回收站列表用，非 reader 表列） */
    private String deletedBy;

    public void setReaderId(Long readerId)
    {
        this.readerId = readerId;
    }

    public Long getReaderId() 
    {
        return readerId;
    }

    public void setReaderName(String readerName) 
    {
        this.readerName = readerName;
    }

    public String getReaderName() 
    {
        return readerName;
    }

    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPwdSet() { return pwdSet; }
    public void setPwdSet(String pwdSet) { this.pwdSet = pwdSet; }
    public String getEmailVerified() { return emailVerified; }
    public void setEmailVerified(String emailVerified) { this.emailVerified = emailVerified; }
    public String getPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(String phoneVerified) { this.phoneVerified = phoneVerified; }

    public void setCardNo(String cardNo)
    {
        this.cardNo = cardNo;
    }

    public String getCardNo() 
    {
        return cardNo;
    }

    public void setReaderType(String readerType) 
    {
        this.readerType = readerType;
    }

    public String getReaderType() 
    {
        return readerType;
    }

    public void setSex(String sex) 
    {
        this.sex = sex;
    }

    public String getSex() 
    {
        return sex;
    }

    public void setBirthDate(Date birthDate) 
    {
        this.birthDate = birthDate;
    }

    public Date getBirthDate() 
    {
        return birthDate;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public java.util.Date getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(java.util.Date lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public Long getRecycleId() { return recycleId; }
    public void setRecycleId(Long recycleId) { this.recycleId = recycleId; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public java.util.Date getDeletedTime() { return deletedTime; }
    public void setDeletedTime(java.util.Date deletedTime) { this.deletedTime = deletedTime; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("readerId", getReaderId())
            .append("readerName", getReaderName())
            .append("phone", getPhone())
            .append("email", getEmail())
            .append("cardNo", getCardNo())
            .append("readerType", getReaderType())
            .append("sex", getSex())
            .append("birthDate", getBirthDate())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
