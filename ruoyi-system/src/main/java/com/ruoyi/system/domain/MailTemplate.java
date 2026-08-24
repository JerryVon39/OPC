package com.ruoyi.system.domain;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邮件场景模板对象 mail_template
 * 占位符 {xxx} 渲染；缺失/停用时由代码内置默认模板兜底（业务不受影响）
 */
public class MailTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板编码（业务场景唯一键，如 register.success） */
    private String code;

    /** 模板名称 */
    private String name;

    /** 邮件主题 */
    private String subject;

    /** HTML 正文（{占位符} 替换） */
    private String content;

    /** 状态(0停用 1启用，停用回退内置默认) */
    private String status;

    /** 可用占位符说明 */
    private String remark;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
