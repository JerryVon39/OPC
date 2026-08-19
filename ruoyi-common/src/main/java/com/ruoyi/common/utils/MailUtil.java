package com.ruoyi.common.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮件发送工具（异步、尽力而为）。
 * <p>
 * 设计原则：邮件只是"通知"，绝不能影响主业务。
 * - 通过独立线程池异步发送，调用方（借书/预约等 Service）不阻塞、不拖长事务；
 * - 发送失败只打日志，不向上抛异常；
 * - mail.enabled=false 或未配置授权码时不发任何邮件（业务照常）。
 */
@Component
public class MailUtil
{
    private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /** 是否启用邮件通知（application.yml spring.mail.enabled） */
    @Value("${spring.mail.enabled:true}")
    private boolean enabled;

    /** 发件人邮箱（存在用户名里，QQ 邮箱发件须与登录账号一致） */
    @Value("${spring.mail.username:}")
    private String from;

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
        if (!enabled)
        {
            log.debug("邮件通知已关闭（mail.enabled=false），跳过发送：{} -> {}", subject, to);
            return;
        }
        if (mailSender == null)
        {
            log.warn("未装配 JavaMailSender，跳过邮件发送：{} -> {}", subject, to);
            return;
        }
        if (to == null || to.trim().isEmpty())
        {
            return; // 读者未留邮箱，不回生活负担
        }
        final String target = to.trim();
        EXEC.submit(() -> {
            try
            {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(from == null || from.isEmpty() ? "241180560@qq.com" : from);
                helper.setTo(target);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(msg);
                log.info("邮件已发送：{} -> {}", subject, target);
            }
            catch (Exception e)
            {
                // 邮件失败不影响业务：仅记录
                log.warn("邮件发送失败：{} -> {}, 原因：{}", subject, target, e.getMessage());
            }
        });
    }
}