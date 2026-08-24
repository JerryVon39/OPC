package com.ruoyi.system.service;

/**
 * 安全验证码服务（邮箱通道，短信预留）：生成/发送/校验/频控
 * <p>
 * 存储：Redis，15 分钟有效，验证一次即作废；
 * 频控：同目标 60 秒 1 条、同 IP 每日 10 条（防轰炸/防刷验证码）。
 */
public interface AuthCodeService
{
    /**
     * 生成 6 位验证码并通过通道发送（异步）
     *
     * @param target  目标（邮箱；短信通道接入后为手机号）
     * @param purpose 用途标识（register / resetPwd / changeEmail）
     * @param ip      请求来源 IP（每日条数限流）
     * @throws com.ruoyi.common.exception.ServiceException 频控拦截或通道未配置时抛出（前端展示）
     */
    void sendCode(String target, String purpose, String ip);

    /**
     * 校验验证码：正确且未过期 → 消费作废返回 true；否则 false
     */
    boolean verify(String target, String purpose, String code);
}
