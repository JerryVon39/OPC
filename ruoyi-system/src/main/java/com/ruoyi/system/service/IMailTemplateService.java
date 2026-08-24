package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.MailTemplate;

/**
 * 邮件场景模板服务：{占位符} 渲染 + 库模板优先、内置默认兜底
 */
public interface IMailTemplateService
{
    /**
     * 按模板编码发送邮件（异步、尽力而为）：渲染占位符 → MailUtil.sendHtml。
     * 库模板缺失/停用时回退内置默认模板；收件邮箱为空不发。
     *
     * @param code   模板编码（如 register.success / borrow.success）
     * @param to     收件人邮箱
     * @param params 占位符值（{key} → 值，渲染前统一 HTML 转义）
     */
    void send(String code, String to, Map<String, Object> params);

    /** 渲染后的模板（subject + content 已完成占位符替换；库缺失/停用回退内置默认） */
    MailTemplate render(String code, Map<String, Object> params);

    /** 模板列表（后台管理） */
    List<MailTemplate> list(MailTemplate query);

    /** 按编码查库模板（后台编辑用，不含内置默认回退） */
    MailTemplate getByCode(String code);

    /** 新增或更新模板（后台编辑，库中存在 code 则更新） */
    int save(MailTemplate mailTemplate);
}
