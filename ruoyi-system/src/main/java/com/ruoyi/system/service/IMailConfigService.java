package com.ruoyi.system.service;

import com.ruoyi.system.domain.MailConfig;

/**
 * 邮件 SMTP 配置服务（后台可改，改完即时生效，不重启）
 */
public interface IMailConfigService
{
    /** 后台读取（授权码不回显，仅标记是否已配置/已加密） */
    MailConfig getConfig();

    /** 后台保存（授权码留空 = 保留原值；非空 = AES 加密后入库） */
    int saveConfig(MailConfig mailConfig);

    /** 测试发送：用当前配置向指定邮箱发一封测试邮件（同步，失败抛异常） */
    void testSend(String to);
}
