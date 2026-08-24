package com.ruoyi.common.utils;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ruoyi.common.core.domain.MailSettings;
import com.ruoyi.common.exception.ServiceException;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮件发送工具（异步、尽力而为）。
 * <p>
 * 设计原则：邮件只是"通知"，绝不能影响主业务。
 * - 通过独立线程池异步发送，调用方（借书/预约等 Service）不阻塞、不拖长事务；
 * - 发送失败只打日志，不向上抛异常；
 * - 配置来源优先级：数据库 mail_config（后台可改，改完即时生效）> 环境变量/application.yml > 内置默认；
 *   未配置发件邮箱/授权码时不发任何邮件（业务照常），启动自检打 warn 提示。
 */
@Component
public class MailUtil
{
    private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

    /** 数据库配置提供者（system 层实现，optional：未装配时回退环境变量） */
    @Autowired(required = false)
    private MailConfigProvider configProvider;

    // ---- 环境变量兜底（无数据库配置时生效，保持改造前行为完全一致） ----
    @Value("${spring.mail.enabled:true}")
    private boolean envEnabled;
    @Value("${spring.mail.host:smtp.qq.com}")
    private String envHost;
    @Value("${spring.mail.port:465}")
    private int envPort;
    @Value("${spring.mail.username:}")
    private String envUsername;
    @Value("${spring.mail.password:}")
    private String envPassword;

    /** 启动自检：未配置发件邮箱/授权码时立即告警，避免"业务成功但邮件发不出"的困惑 */
    @jakarta.annotation.PostConstruct
    public void checkConfig()
    {
        try
        {
            MailSettings s = resolveSettings();
            if (!s.isEnabled())
            {
                return;
            }
            if (s.getUsername() == null || s.getUsername().trim().isEmpty())
            {
                log.warn("邮件通知已启用但未配置发件邮箱（后台“邮件通知”页或 MAIL_USERNAME 环境变量为空），邮件将不会发送！");
            }
            else if (s.getAuthCode() == null || s.getAuthCode().isEmpty())
            {
                log.warn("邮件通知已启用但未配置 SMTP 授权码（后台“邮件通知”页或 MAIL_AUTH_CODE 环境变量为空），邮件将发送失败！");
            }
        }
        catch (Exception e)
        {
            // 数据库配置表不存在/读取失败等：不影响启动，下次发送时再兜底
            log.warn("邮件配置读取失败（首次部署未执行升级脚本？），使用环境变量配置：{}", e.getMessage());
        }
    }

    /** 当前生效配置：数据库优先，失败/未装配回退环境变量，永不抛异常 */
    private MailSettings resolveSettings()
    {
        try
        {
            if (configProvider != null)
            {
                MailSettings s = configProvider.get();
                if (s != null)
                {
                    return s;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("数据库邮件配置读取失败，回退环境变量配置：{}", e.getMessage());
        }
        MailSettings s = new MailSettings();
        s.setEnabled(envEnabled);
        s.setHost(envHost);
        s.setPort(envPort);
        s.setUsername(envUsername);
        s.setAuthCode(envPassword);
        return s;
    }

    /** 动态构建发送器：配置在后台修改后下次发送立即生效（不重启） */
    private JavaMailSenderImpl buildSender(MailSettings s)
    {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.getHost());
        sender.setPort(s.getPort());
        sender.setUsername(s.getUsername());
        sender.setPassword(s.getAuthCode());
        // QQ 邮箱要求 SSL：port 465 + mail.smtp.ssl.enable；jakarta.mail 2.x 已移除 SocketFactory，
        // 不要再配 socketFactory.class（会导致 ClassNotFoundException、邮件静默发送失败）
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        sender.setJavaMailProperties(props);
        return sender;
    }

    /** 独立小线程池：异步发信，避免干扰 Web 请求线程/主业务事务 */
    private static final ExecutorService EXEC = new ThreadPoolExecutor(
            1, 2, 10L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> { Thread t = new Thread(r, "mail-sender"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.DiscardPolicy());

    /**
     * 发送 HTML 邮件（异步）。所有异常吞掉，仅供业务侧"尽力而为"通知。
     *
     * @param to      收件人邮箱（空则直接忽略，不发送）
     * @param subject 主题
     * @param html    正文（HTML）
     */
    public void sendHtml(String to, String subject, String html)
    {
        final MailSettings s = resolveSettings();
        if (!s.isEnabled())
        {
            log.debug("邮件通知已关闭（mail.enabled=false），跳过发送：{} -> {}", subject, to);
            return;
        }
        if (to == null || to.trim().isEmpty())
        {
            return; // 读者未留邮箱，不回生活负担
        }
        // 发件邮箱必须配置（后台/MAIL_USERNAME），不硬编码任何真实账号
        if (s.getUsername() == null || s.getUsername().trim().isEmpty())
        {
            log.warn("未配置发件邮箱（后台“邮件通知”页/MAIL_USERNAME），跳过邮件发送：{} -> {}", subject, to);
            return;
        }
        final String target = to.trim();
        EXEC.submit(() -> {
            try
            {
                MimeMessage msg = buildSender(s).createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(s.getUsername().trim());
                helper.setTo(target);
                helper.setSubject(subject);
                helper.setText(html, true);
                buildSender(s).send(msg);
                log.info("邮件已发送：{} -> {}", subject, target);
            }
            catch (Exception e)
            {
                // 邮件失败不影响业务：仅记录
                log.warn("邮件发送失败：{} -> {}, 原因：{}", subject, target, e.getMessage());
            }
        });
    }

    /**
     * 发送 HTML 邮件（同步）。供管理端"测试发送"等需要即时结果的场景使用。
     * 失败抛 ServiceException（调用方展示给管理员），不静默。
     *
     * @param to      收件人邮箱
     * @param subject 主题
     * @param html    正文（HTML）
     */
    public void sendSyncHtml(String to, String subject, String html)
    {
        MailSettings s = resolveSettings();
        if (!s.isEnabled())
        {
            throw new ServiceException("邮件通知已关闭，请先在“邮件通知”页开启");
        }
        if (to == null || to.trim().isEmpty())
        {
            throw new ServiceException("收件人邮箱不能为空");
        }
        if (s.getUsername() == null || s.getUsername().trim().isEmpty())
        {
            throw new ServiceException("未配置发件邮箱，请先在“邮件通知”页填写");
        }
        if (s.getAuthCode() == null || s.getAuthCode().isEmpty())
        {
            throw new ServiceException("未配置 SMTP 授权码，请先在“邮件通知”页填写");
        }
        try
        {
            MimeMessage msg = buildSender(s).createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(s.getUsername().trim());
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(html, true);
            buildSender(s).send(msg);
            log.info("测试邮件已发送：{} -> {}", subject, to.trim());
        }
        catch (Exception e)
        {
            throw new ServiceException("邮件发送失败：" + e.getMessage());
        }
    }
}
