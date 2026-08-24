package com.ruoyi.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.MailConfig;
import com.ruoyi.system.service.IMailConfigService;

/**
 * 邮件 SMTP 配置 Controller（后台"邮件通知"页）
 */
@RestController
@RequestMapping("/system/mail/config")
public class MailConfigController extends BaseController
{
    @Autowired
    private IMailConfigService mailConfigService;

    /** 读取配置（授权码不回显） */
    @PreAuthorize("@ss.hasPermi('system:mail:config')")
    @GetMapping
    public AjaxResult getConfig()
    {
        return success(mailConfigService.getConfig());
    }

    /** 保存配置（授权码留空 = 保留原值；改完即时生效不重启） */
    @PreAuthorize("@ss.hasPermi('system:mail:config')")
    @Log(title = "邮件配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult saveConfig(@RequestBody MailConfig mailConfig)
    {
        mailConfig.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(mailConfigService.saveConfig(mailConfig));
    }

    /** 测试发送：用当前配置向指定邮箱发一封测试邮件（同步返回结果） */
    @PreAuthorize("@ss.hasPermi('system:mail:config')")
    @Log(title = "邮件配置", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public AjaxResult testSend(@RequestParam String to)
    {
        mailConfigService.testSend(to);
        return success("测试邮件已发送至 " + to);
    }
}
