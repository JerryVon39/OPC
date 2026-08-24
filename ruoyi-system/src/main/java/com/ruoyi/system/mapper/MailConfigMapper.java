package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.MailConfig;

/**
 * 邮件 SMTP 配置 Mapper（单行表）
 */
public interface MailConfigMapper
{
    /** 读取配置（单行 id=1，无行返回 null） */
    MailConfig selectConfig();

    /** 更新配置（单行） */
    int updateConfig(MailConfig mailConfig);
}
