package com.ruoyi.common.core.domain;

/**
 * 邮件运行时配置（MailConfigProvider 返回）。
 * 来源优先级：数据库 mail_config（后台可改）> 环境变量/application.yml > 内置默认。
 */
public class MailSettings
{
    /** 是否启用邮件通知 */
    private boolean enabled = true;

    /** SMTP 主机 */
    private String host = "smtp.qq.com";

    /** SMTP 端口 */
    private int port = 465;

    /** 发件邮箱 */
    private String username = "";

    /** SMTP 授权码（已解密，仅内存使用，不落日志） */
    private String authCode = "";

    /** 发件人昵称（可选，为空则用邮箱） */
    private String fromName = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
}
