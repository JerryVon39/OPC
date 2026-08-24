package com.ruoyi.system.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.system.service.IMailTemplateService;
import com.ruoyi.system.service.VerificationSender;

/**
 * 邮箱验证码通道：走邮件模板 auth.code（异步、尽力而为）
 */
@Component
public class MailVerificationSender implements VerificationSender
{
    @Autowired
    private IMailTemplateService mailTemplateService;

    @Override
    public void send(String target, String code, int ttlMinutes)
    {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("minutes", ttlMinutes);
        mailTemplateService.send("auth.code", target, params);
    }
}
