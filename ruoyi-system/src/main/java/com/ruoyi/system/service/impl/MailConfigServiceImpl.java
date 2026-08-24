package com.ruoyi.system.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.domain.MailSettings;
import com.ruoyi.common.utils.AesUtil;
import com.ruoyi.common.utils.MailConfigProvider;
import com.ruoyi.common.utils.MailUtil;
import com.ruoyi.system.domain.MailConfig;
import com.ruoyi.system.mapper.MailConfigMapper;
import com.ruoyi.system.service.IMailConfigService;

/**
 * 邮件 SMTP 配置服务：数据库优先，环境变量兜底，改完即时生效
 */
@Service
public class MailConfigServiceImpl implements IMailConfigService, MailConfigProvider
{
    @Autowired
    private MailConfigMapper mailConfigMapper;

    @Autowired
    private MailUtil mailUtil;

    // ---- 环境变量兜底（数据库未配置时生效，保持与改造前行为一致） ----
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

    /** MailConfigProvider 实现：MailUtil 每次发送前调用；读库异常向上抛，由 MailUtil 侧兜底回退环境变量 */
    @Override
    public MailSettings get()
    {
        MailConfig cfg = mailConfigMapper.selectConfig();
        MailSettings s = new MailSettings();
        if (cfg == null)
        {
            // 表存在但无行（异常态）：整体回退环境变量
            s.setEnabled(envEnabled);
            s.setHost(envHost);
            s.setPort(envPort);
            s.setUsername(envUsername);
            s.setAuthCode(envPassword);
            return s;
        }
        s.setEnabled("1".equals(cfg.getEnabled()));
        s.setHost(cfg.getHost() == null || cfg.getHost().trim().isEmpty() ? envHost : cfg.getHost().trim());
        s.setPort(cfg.getPort() == null ? envPort : cfg.getPort());
        String user = cfg.getUsername() == null ? "" : cfg.getUsername().trim();
        s.setUsername(user.isEmpty() ? envUsername : user);
        String auth = AesUtil.decrypt(cfg.getAuthCode());
        s.setAuthCode(auth == null || auth.isEmpty() ? envPassword : auth);
        s.setFromName(cfg.getFromName() == null ? "" : cfg.getFromName());
        return s;
    }

    /** 后台读取：授权码不回显，仅返回是否已配置/已加密的标记 */
    @Override
    public MailConfig getConfig()
    {
        MailConfig cfg = mailConfigMapper.selectConfig();
        if (cfg == null)
        {
            cfg = new MailConfig();
        }
        String auth = cfg.getAuthCode();
        cfg.setAuthCode(null); // 绝不在接口回显授权码
        cfg.setAuthCodeConfigured(auth != null && !auth.isEmpty());
        cfg.setAuthCodeEncrypted(auth != null && auth.startsWith("enc:"));
        return cfg;
    }

    /** 后台保存：授权码留空 = 保留原值；非空 = AES 加密后入库 */
    @Override
    public int saveConfig(MailConfig mailConfig)
    {
        if (mailConfig.getAuthCode() == null || mailConfig.getAuthCode().trim().isEmpty())
        {
            MailConfig old = mailConfigMapper.selectConfig();
            mailConfig.setAuthCode(old == null ? "" : old.getAuthCode());
        }
        else
        {
            mailConfig.setAuthCode(AesUtil.encrypt(mailConfig.getAuthCode().trim()));
        }
        if (mailConfig.getFromName() != null)
        {
            mailConfig.setFromName(mailConfig.getFromName().trim());
        }
        mailConfig.setUpdateTime(new Date());
        return mailConfigMapper.updateConfig(mailConfig);
    }

    /** 测试发送：同步直发，配置错误立即暴露给管理员 */
    @Override
    public void testSend(String to)
    {
        mailUtil.sendSyncHtml(to, "【数智游民创新工场】测试邮件",
                "<p>这是一封测试邮件，您的 SMTP 配置已生效，可以正常发送通知。</p>"
                + "<p>—— 数智游民创新工场</p>");
    }
}
