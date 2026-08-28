package com.ruoyi.common.utils;

import com.ruoyi.common.core.domain.MailSettings;

/**
 * 邮件配置提供者（由 ruoyi-system 的 MailConfigServiceImpl 实现）。
 * <p>
 * 解耦原因：MailUtil 位于 ruoyi-common，不能反向依赖 system 的数据库服务；
 * 通过本接口 + Spring 注入（optional），system 层注册实现即可——未装配时 MailUtil 回退环境变量配置。
 * 读取失败时的兜底由 MailUtil 侧 try-catch 承担（实现方 get() 允许抛异常，MailUtil 捕获后回退环境变量）。
 */
public interface MailConfigProvider
{
    /** 返回当前生效的邮件配置（已解密授权码），不允许返回 null */
    MailSettings get();
}
