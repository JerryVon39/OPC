package com.ruoyi.system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.MailTemplate;
import com.ruoyi.system.service.IMailTemplateService;

/**
 * 邮件场景模板 Controller（后台"邮件通知"页，模板编辑）
 */
@RestController
@RequestMapping("/system/mail/template")
public class MailTemplateController extends BaseController
{
    @Autowired
    private IMailTemplateService mailTemplateService;

    /** 模板列表 */
    @PreAuthorize("@ss.hasPermi('system:mail:template')")
    @GetMapping("/list")
    public AjaxResult list(MailTemplate query)
    {
        List<MailTemplate> list = mailTemplateService.list(query);
        return success(list);
    }

    /** 模板详情（编辑回显，含正文） */
    @PreAuthorize("@ss.hasPermi('system:mail:template')")
    @GetMapping("/{code}")
    public AjaxResult getInfo(@PathVariable("code") String code)
    {
        return success(mailTemplateService.getByCode(code));
    }

    /** 保存模板（不存在则新增，存在则更新；改完即时生效） */
    @PreAuthorize("@ss.hasPermi('system:mail:template')")
    @Log(title = "邮件模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult save(@RequestBody MailTemplate mailTemplate)
    {
        mailTemplate.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(mailTemplateService.save(mailTemplate));
    }
}
