package com.ruoyi.system.mapper;

import java.util.List;

import com.ruoyi.system.domain.MailTemplate;

/**
 * 邮件场景模板 Mapper
 */
public interface MailTemplateMapper
{
    /** 模板列表（后台管理） */
    List<MailTemplate> selectMailTemplateList(MailTemplate mailTemplate);

    /** 按编码查模板 */
    MailTemplate selectMailTemplateByCode(String code);

    /** 新增模板（后台编辑） */
    int insertMailTemplate(MailTemplate mailTemplate);

    /** 更新模板（后台编辑） */
    int updateMailTemplate(MailTemplate mailTemplate);
}
