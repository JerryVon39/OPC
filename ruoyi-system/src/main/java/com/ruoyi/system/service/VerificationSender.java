package com.ruoyi.system.service;

/**
 * 验证码发送通道（抽象，短信通道预留）。
 * <p>
 * 当前实现：MailVerificationSender（邮箱，模板 auth.code）。
 * 预留：接入短信服务商时新增 SmsVerificationSender 实现本接口，
 * 并按 sys_config sms.enabled=true 切换——AuthCodeService 侧无需改动。
 */
public interface VerificationSender
{
    /**
     * 向目标（邮箱/手机号）发送验证码
     *
     * @param target    目标地址（邮箱或手机号）
     * @param code      6 位验证码
     * @param ttlMinutes 有效分钟数（模板文案展示用）
     */
    void send(String target, String code, int ttlMinutes);
}
