package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.utils.MailUtil;
import com.ruoyi.system.domain.MailTemplate;
import com.ruoyi.system.mapper.MailTemplateMapper;
import com.ruoyi.system.service.IMailTemplateService;

/**
 * 邮件场景模板服务：库模板优先、内置默认兜底（表未初始化/模板被删/停用都不影响发信）
 */
@Service
@Transactional
public class MailTemplateServiceImpl implements IMailTemplateService
{
    private static final Logger log = LoggerFactory.getLogger(MailTemplateServiceImpl.class);

    @Autowired
    private MailTemplateMapper mailTemplateMapper;

    @Autowired
    private MailUtil mailUtil;

    /**
     * 内置默认模板（与 upgrade_20260824_auth.sql 初始数据保持一致）：
     * code → [主题, HTML 正文]，{占位符} 渲染
     */
    private static final Map<String, String[]> DEFAULT_TEMPLATES = new HashMap<>();
    static
    {
        DEFAULT_TEMPLATES.put("register.success", new String[] {
                "【数智游民创新工场】欢迎加入，您的成员编号已生成",
                "<p>您好，{readerName}：</p><p>欢迎加入数智游民创新工场！您的成员编号为 <b>{cardNo}</b>。</p>"
                + "<p>请使用「成员编号 + 密码」登录官网，妥善保管编号，丢失可在前台补办。</p>"
                + "<p>感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("reissue.notify", new String[] {
                "【数智游民创新工场】您的成员编号已更新",
                "<p>您好，{readerName}：</p><p>您申请补办的成员编号已生效，新编号为 <b>{cardNo}</b>，旧编号已作废。</p>"
                + "<p>请使用新编号 + 原密码登录。感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("auth.code", new String[] {
                "【数智游民创新工场】安全验证码",
                "<p>您好：</p><p>您的验证码为 <b>{code}</b>，{minutes} 分钟内有效，请勿泄露给他人。</p>"
                + "<p>如非本人操作，请忽略本邮件。感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("borrow.success", new String[] {
                "【服务报名】报名成功",
                "<p>您好，{readerName}：</p><p>您已成功报名《{bookName}》，截止日期为 {dueDate}。</p>"
                + "<p>请按时完成，逾期将影响后续报名。感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("renew.success", new String[] {
                "【服务报名】续期成功",
                "<p>您好，{readerName}：</p><p>您已成功续期《{bookName}》，新的应还日期为 {dueDate}。</p>"
                + "<p>如再次需续期或有其他问题，请联系服务台。感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("reserve.success", new String[] {
                "【服务候补】候补成功",
                "<p>您好，{readerName}：</p><p>您已成功预约《{bookName}》。该服务当前无名额，已进入候补队列；</p>"
                + "<p>一旦有名额释放，我们会通过邮件通知您。感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("reserve.available", new String[] {
                "【服务候补】您候补的服务有名额了",
                "<p>您好，{readerName}：</p><p>您候补的《{bookName}》已有名额，现可前来报名。</p>"
                + "<p>请尽早在 {days} 天内办理报名，逾期未办将视为放弃。感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("reserve.cancel", new String[] {
                "【服务候补】候补名额已释放",
                "<p>您好，{readerName}：</p><p>您候补的《{bookName}》名额已释放，候补状态已自动取消。您仍可重新报名或继续候补。</p>"
                + "<p>感谢支持数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("purchase.pass", new String[] {
                "【入驻申请】您的入驻申请已通过",
                "<p>您好：</p><p>您的申请《{applyName}》已通过审核，运营团队将尽快与您联系办理入驻。欢迎加入数智游民创新工场！</p>" });
        DEFAULT_TEMPLATES.put("purchase.reject", new String[] {
                "【入驻申请】您的入驻申请未通过",
                "<p>您好：</p><p>很遗憾，您的申请《{applyName}》暂未通过审核。我们会持续关注您的需求，感谢支持！</p>" });
    }

    @Override
    public void send(String code, String to, Map<String, Object> params)
    {
        MailTemplate tpl = render(code, params);
        if (tpl == null)
        {
            log.warn("未知邮件模板编码：{}，跳过发送：{}", code, to);
            return;
        }
        mailUtil.sendHtml(to, tpl.getSubject(), tpl.getContent());
    }

    /** 渲染：库模板优先（状态=1），缺失/停用回退内置默认；占位符值统一 HTML 转义 */
    @Override
    public MailTemplate render(String code, Map<String, Object> params)
    {
        MailTemplate tpl = null;
        try
        {
            tpl = mailTemplateMapper.selectMailTemplateByCode(code);
        }
        catch (Exception e)
        {
            log.warn("邮件模板读取失败（升级脚本未执行？），使用内置默认：{}", e.getMessage());
        }
        String subject;
        String content;
        if (tpl == null || !"1".equals(tpl.getStatus()))
        {
            String[] def = DEFAULT_TEMPLATES.get(code);
            if (def == null)
            {
                return null;
            }
            subject = def[0];
            content = def[1];
        }
        else
        {
            subject = tpl.getSubject();
            content = tpl.getContent();
        }
        MailTemplate out = new MailTemplate();
        out.setCode(code);
        out.setSubject(replace(subject, params));
        out.setContent(replace(content, params));
        return out;
    }

    /** {key} → 值（HTML 转义，防止书名/姓名/验证码注入破坏模板或引入 XSS） */
    private String replace(String text, Map<String, Object> params)
    {
        if (text == null)
        {
            return "";
        }
        if (params == null || params.isEmpty())
        {
            return text;
        }
        for (Map.Entry<String, Object> e : params.entrySet())
        {
            String key = "{" + e.getKey() + "}";
            if (text.contains(key))
            {
                Object v = e.getValue();
                text = text.replace(key, v == null ? "" : esc(String.valueOf(v)));
            }
        }
        return text;
    }

    private String esc(String s)
    {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public List<MailTemplate> list(MailTemplate query)
    {
        return mailTemplateMapper.selectMailTemplateList(query);
    }

    @Override
    public MailTemplate getByCode(String code)
    {
        return mailTemplateMapper.selectMailTemplateByCode(code);
    }

    /** 新增或更新（存在则更新，不存在则插入） */
    @Override
    public int save(MailTemplate mailTemplate)
    {
        mailTemplate.setUpdateTime(new Date());
        if (mailTemplateMapper.selectMailTemplateByCode(mailTemplate.getCode()) != null)
        {
            return mailTemplateMapper.updateMailTemplate(mailTemplate);
        }
        return mailTemplateMapper.insertMailTemplate(mailTemplate);
    }
}
