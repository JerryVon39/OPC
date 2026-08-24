package com.ruoyi.system.domain;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邮件 SMTP 配置对象 mail_config（单行 id=1，后台可改，改完即时生效）
 */
public class MailConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键（固定 1） */
    private Long id;

    /** 邮件总开关(0关 1开) */
    private String enabled;

    /** SMTP 主机 */
    private String host;

    /** SMTP 端口 */
    private Integer port;

    /** 发件邮箱 */
    private String username;

    /** SMTP 授权码（AES 加密入库；接口读取时不回显，仅用下面两个标记） */
    private String authCode;

    /** 发件人昵称（可选） */
    private String fromName;

    /** 是否已配置授权码（页面提示用，不回显明文） */
    private boolean authCodeConfigured;

    /** 授权码是否已加密存储（未设 MAIL_SECRET_KEY 时为明文，页面警告） */
    private boolean authCodeEncrypted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public boolean isAuthCodeConfigured() { return authCodeConfigured; }
    public void setAuthCodeConfigured(boolean authCodeConfigured) { this.authCodeConfigured = authCodeConfigured; }
    public boolean isAuthCodeEncrypted() { return authCodeEncrypted; }
    public void setAuthCodeEncrypted(boolean authCodeEncrypted) { this.authCodeEncrypted = authCodeEncrypted; }
}
