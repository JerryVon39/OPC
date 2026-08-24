package com.ruoyi.system.domain;

/**
 * 管理员直接设置密码请求体
 */
public class SetPasswordBody
{
    /** 成员 ID */
    private Long readerId;

    /** 新密码（明文，BCrypt 加密后落库） */
    private String newPassword;

    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
