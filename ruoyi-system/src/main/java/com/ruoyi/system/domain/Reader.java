package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 读者管理对象 reader
 * 
 * @author Jerry
 * @date 2026-08-12
 */
public class Reader extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 读者ID */
    private Long readerId;

    /** 读者姓名 */
    @Excel(name = "读者姓名")
    private String readerName;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phone;

    /** 借书证号 */
    @Excel(name = "借书证号")
    private String cardNo;

    /** 读者类型 */
    @Excel(name = "读者类型")
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("readerId", getReaderId())
            .append("readerName", getReaderName())
            .append("phone", getPhone())
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
