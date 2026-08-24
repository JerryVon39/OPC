package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.domain.ReaderSession;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.ReaderSessionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.service.AuthCodeService;
import com.ruoyi.common.utils.PasswordStrength;
import com.ruoyi.common.utils.ip.IpUtils;

/**
 * 成员管理Controller
 * 
 * @author Jerry
 * @date 2026-08-12
 */
@RestController
@RequestMapping("/system/reader")
public class ReaderController extends BaseController
{
    @Autowired
    private IReaderService readerService;

    @Autowired
    private ReaderSessionService readerSessionService;

    @Autowired
    private AuthCodeService authCodeService;

    @Autowired
    private com.ruoyi.system.service.IMailTemplateService mailTemplateService;

    /** 客户端设备标识（User-Agent 摘要，多端管理展示用；不存在返回空串） */
    private String deviceOf(jakarta.servlet.http.HttpServletRequest request)
    {
        String ua = request == null ? null : request.getHeader("User-Agent");
        if (ua == null || ua.trim().isEmpty())
        {
            return "";
        }
        return ua.length() > 60 ? ua.substring(0, 60) : ua;
    }

    /** 当前请求来源 IP */
    private String ipOf(jakarta.servlet.http.HttpServletRequest request)
    {
        return request == null ? "" : IpUtils.getIpAddr(request);
    }

    /**
     * 查询成员管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:reader:list')")
    @GetMapping("/list")
    public TableDataInfo list(Reader reader)
    {
        startPage();
        List<Reader> list = readerService.selectReaderList(reader);
        return getDataTable(list);
    }

    /**
     * 导出成员管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:reader:export')")
    @Log(title = "读者管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Reader reader)
    {
        List<Reader> list = readerService.selectReaderList(reader);
        ExcelUtil<Reader> util = new ExcelUtil<Reader>(Reader.class);
        util.exportExcel(response, list, "读者管理数据");
    }

    /**
     * 批量导入成员（Excel）：逐行校验，证号判重/留空自动生成，返回成功/失败明细
     */
    @PreAuthorize("@ss.hasPermi('system:reader:add')")
    @Log(title = "读者管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(org.springframework.web.multipart.MultipartFile file) throws Exception
    {
        ExcelUtil<Reader> util = new ExcelUtil<Reader>(Reader.class);
        List<Reader> list = util.importExcel(file.getInputStream());
        return success(readerService.importReaders(list));
    }

    /**
     * 下载成员导入模板
     */
    @PreAuthorize("@ss.hasPermi('system:reader:add')")
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Reader> util = new ExcelUtil<Reader>(Reader.class);
        util.importTemplateExcel(response, "读者管理数据");
    }

    /**
     * 获取成员管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:reader:query')")
    @GetMapping(value = "/{readerId}")
    public AjaxResult getInfo(@PathVariable("readerId") Long readerId)
    {
        return success(readerService.selectReaderByReaderId(readerId));
    }

    /**
     * 新增成员管理
     */
    /** 前台成员登录（匿名）：成员编号 + 密码
     * 错误提示统一"成员编号或密码不正确"（不暴露证号是否存在，防枚举），
     * 并按 IP+证号 维度频控（30 分钟窗口内失败 5 次拦截，防爆破）；
     * 未设置密码的存量成员（pwd_set=0）返回专用状态码 601，前端引导「邮箱验证 → 设置密码」 */
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(String cardNo, String password, jakarta.servlet.http.HttpServletRequest request)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || password == null || password.isEmpty())
        {
            return error("请输入成员编号和密码");
        }
        String failKey = "login:" + ipOf(request) + ":" + cardNo.trim();
        if (readerSessionService.isBlocked(failKey))
        {
            return error("尝试次数过多，请 30 分钟后再试");
        }
        Reader auth = readerService.findAuthByCardNo(cardNo.trim());
        if (auth == null || auth.getPasswordHash() == null
                || !com.ruoyi.common.utils.SecurityUtils.matchesPassword(password, auth.getPasswordHash()))
        {
            readerSessionService.recordFail(failKey);
            return error("成员编号或密码不正确");
        }
        // 停用/挂失的证号不允许登录前台（报名、下单前就把问题拦下）
        if (!"0".equals(auth.getStatus()))
        {
            return error("该成员编号已停用/挂失，请联系管理员");
        }
        // 未设置密码（存量成员迁移）：返回专用状态码，前端引导设密流程
        if (!"1".equals(auth.getPwdSet()))
        {
            AjaxResult setup = AjaxResult.success();
            setup.put("code", 601);
            setup.put("msg", "首次登录请先验证邮箱并设置密码");
            setup.put("data", "AUTH_SETUP_REQUIRED");
            return setup;
        }
        readerSessionService.clearFail(failKey);
        readerService.touchLogin(auth.getReaderId());
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("readerId", auth.getReaderId());
        result.put("readerName", auth.getReaderName());
        result.put("cardNo", auth.getCardNo());
        result.put("readerType", auth.getReaderType());
        result.put("status", auth.getStatus());
        result.put("emailVerified", "1".equals(auth.getEmailVerified()));
        result.put("sessionToken", readerSessionService.create(auth.getCardNo(), ipOf(request), deviceOf(request)));
        return success(result);
    }

    /** 前台自助登记 · 第一步：资料 + 密码（匿名）
     * 证号由后端生成（防伪造/占用）；密码强度校验后 BCrypt 落库；
     * 落库后向邮箱发 6 位验证码，前端进入第二步 verify-email 完成注册。
     * 频控：按 IP 维度（30 分钟窗口内失败 5 次拦截，防脚本刷库灌垃圾成员） */
    @Anonymous
    @PostMapping("/register")
    public AjaxResult register(String readerName, String phone, String readerType, String email, String remark,
            String password, jakarta.servlet.http.HttpServletRequest request)
    {
        String failKey = "register:" + ipOf(request);
        if (readerSessionService.isBlocked(failKey))
        {
            return error("尝试次数过多，请 30 分钟后再试");
        }
        if (readerName == null || readerName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()
                || readerType == null || readerType.trim().isEmpty())
        {
            readerSessionService.recordFail(failKey);
            return error("请填写姓名、手机号和读者类型");
        }
        if (!phone.trim().matches("\\d{11}"))
        {
            readerSessionService.recordFail(failKey);
            return error("手机号格式不正确（需 11 位数字）");
        }
        String em = trimEmail(email);
        if (em == null)
        {
            readerSessionService.recordFail(failKey);
            return error("请填写有效的电子邮箱（用于接收报名/候补通知）");
        }
        // 密码强度校验（≥10 位、至少 3 类字符）
        try
        {
            PasswordStrength.check(password);
        }
        catch (Exception e)
        {
            readerSessionService.recordFail(failKey);
            return error(e.getMessage());
        }
        readerSessionService.clearFail(failKey);
        Reader r = readerService.register(readerName.trim(), phone.trim(), readerType.trim(), em, remark, password);
        // 发验证码邮件（注册第二步用；频控 60s/条，失败不阻塞注册，可稍后重发）
        try
        {
            authCodeService.sendCode(em, "register", ipOf(request));
        }
        catch (Exception e)
        {
            // 邮件通道不可用时注册仍成功，前端提示"验证码发送失败可稍后重发"
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("readerId", r.getReaderId());
        result.put("readerName", r.getReaderName());
        result.put("cardNo", r.getCardNo());
        result.put("email", r.getEmail());
        result.put("step", "verify_email");
        return success(result);
    }

    /** 前台自助登记 · 第二步：邮箱验证码完成注册（匿名）
     * 校验通过 → email_verified=1 → 发送注册成功邮件（含证号）→ 引导登录 */
    @Anonymous
    @PostMapping("/verify-email")
    public AjaxResult verifyEmail(String cardNo, String email, String code, jakarta.servlet.http.HttpServletRequest request)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || email == null || email.trim().isEmpty()
                || code == null || code.trim().isEmpty())
        {
            return error("参数不完整");
        }
        Reader auth = readerService.findAuthByCardNo(cardNo.trim());
        if (auth == null || !email.trim().equalsIgnoreCase(auth.getEmail()))
        {
            return error("邮箱与注册信息不匹配");
        }
        if (!authCodeService.verify(auth.getEmail(), "register", code))
        {
            return error("验证码不正确或已过期，请重新获取");
        }
        readerService.verifyEmail(auth.getCardNo());
        // 注册成功邮件：只发证号，绝不含密码明文（模板 register.success）
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("readerName", auth.getReaderName());
        params.put("cardNo", auth.getCardNo());
        mailTemplateService.send("register.success", auth.getEmail(), params);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("cardNo", auth.getCardNo());
        result.put("emailVerified", true);
        result.put("msg", "注册成功，请使用成员编号 + 密码登录");
        return success(result);
    }

    /** 重发验证码（注册/找回通用）：频控由 AuthCodeService 兜底（60s 一条、IP 每日 10 条） */
    @Anonymous
    @PostMapping("/resend-code")
    public AjaxResult resendCode(String target, String purpose, jakarta.servlet.http.HttpServletRequest request)
    {
        String em = trimEmail(target);
        if (em == null)
        {
            return error("请输入有效的邮箱");
        }
        try
        {
            authCodeService.sendCode(em, purpose == null ? "register" : purpose.trim(), ipOf(request));
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
        return success("验证码已发送，请查收邮件");
    }

    /** 登出（匿名接口，会话令牌鉴权）：删除当前会话 */
    @Anonymous
    @PostMapping("/logout")
    public AjaxResult logout(jakarta.servlet.http.HttpServletRequest request)
    {
        String token = request.getHeader("X-Session-Token");
        if (token == null || token.trim().isEmpty())
        {
            token = request.getParameter("sessionToken");
        }
        readerSessionService.remove(token);
        return success("已退出登录");
    }

    /** 找回密码 · 第一步（匿名）：按证号向登记邮箱发验证码。
     * 统一提示"验证码已发送至登记邮箱"（不暴露证号是否存在，防枚举） */
    @Anonymous
    @PostMapping("/forgot-password")
    public AjaxResult forgotPassword(String cardNo, jakarta.servlet.http.HttpServletRequest request)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            return error("请输入成员编号");
        }
        Reader auth = readerService.findAuthByCardNo(cardNo.trim());
        if (auth != null && "0".equals(auth.getStatus()) && auth.getEmail() != null && !auth.getEmail().isEmpty())
        {
            try
            {
                authCodeService.sendCode(auth.getEmail(), "resetPwd", ipOf(request));
            }
            catch (Exception e)
            {
                return error(e.getMessage());
            }
        }
        return success("验证码已发送至登记邮箱，请查收");
    }

    /** 找回密码 · 第二步（匿名）：验证码 + 新密码 → 重置成功并强制登出全部会话 */
    @Anonymous
    @PostMapping("/reset-password")
    public AjaxResult resetPassword(String cardNo, String code, String newPassword, jakarta.servlet.http.HttpServletRequest request)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || code == null || code.trim().isEmpty())
        {
            return error("参数不完整");
        }
        try
        {
            PasswordStrength.check(newPassword);
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
        Reader auth = readerService.findAuthByCardNo(cardNo.trim());
        if (auth == null || auth.getEmail() == null || auth.getEmail().isEmpty())
        {
            return error("成员编号不存在或未登记邮箱");
        }
        if (!authCodeService.verify(auth.getEmail(), "resetPwd", code))
        {
            return error("验证码不正确或已过期，请重新获取");
        }
        readerService.setPassword(auth.getCardNo(), newPassword);
        // 强制登出所有会话（含其他设备），防止旧会话在密码重置后仍可用
        readerSessionService.revokeOthers(auth.getCardNo(), null);
        return success("密码已重置，请重新登录");
    }

    /** 修改密码（匿名接口，会话令牌鉴权）：旧密码验证 → 更新 → 退出其他设备（保留当前） */
    @Anonymous
    @PostMapping("/change-password")
    public AjaxResult changePassword(String oldPassword, String newPassword, jakarta.servlet.http.HttpServletRequest request)
    {
        String cardNo = readerSessionService.resolveFromRequest(request);
        if (cardNo == null)
        {
            return error("登录已失效，请重新登录");
        }
        if (oldPassword == null || oldPassword.isEmpty())
        {
            return error("请输入原密码");
        }
        try
        {
            PasswordStrength.check(newPassword);
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
        if (oldPassword.equals(newPassword))
        {
            return error("新密码不能与原密码相同");
        }
        try
        {
            readerService.changePassword(cardNo, oldPassword, newPassword);
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
        // 改密后退出其他设备（防已泄露会话继续使用），当前设备保留
        readerSessionService.revokeOthers(cardNo, request.getHeader("X-Session-Token"));
        return success("密码修改成功");
    }

    /** 修改邮箱 · 第一步（会话鉴权）：向新邮箱发验证码（频控兜底） */
    @Anonymous
    @PostMapping("/send-change-email-code")
    public AjaxResult sendChangeEmailCode(String newEmail, jakarta.servlet.http.HttpServletRequest request)
    {
        String cardNo = readerSessionService.resolveFromRequest(request);
        if (cardNo == null)
        {
            return error("登录已失效，请重新登录");
        }
        String em = trimEmail(newEmail);
        if (em == null)
        {
            return error("请输入有效的邮箱");
        }
        try
        {
            authCodeService.sendCode(em, "changeEmail", ipOf(request));
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
        return success("验证码已发送至新邮箱");
    }

    /** 修改邮箱 · 第二步（会话鉴权）：验证码校验 → 更新邮箱（重置验证状态） */
    @Anonymous
    @PostMapping("/change-email")
    public AjaxResult changeEmail(String newEmail, String code, jakarta.servlet.http.HttpServletRequest request)
    {
        String cardNo = readerSessionService.resolveFromRequest(request);
        if (cardNo == null)
        {
            return error("登录已失效，请重新登录");
        }
        String em = trimEmail(newEmail);
        if (em == null || code == null || code.trim().isEmpty())
        {
            return error("参数不完整");
        }
        if (!authCodeService.verify(em, "changeEmail", code))
        {
            return error("验证码不正确或已过期，请重新获取");
        }
        try
        {
            readerService.changeEmail(cardNo, em);
        }
        catch (Exception e)
        {
            return error(e.getMessage());
        }
        return success("邮箱修改成功");
    }

    /** 会话列表（会话鉴权）：多端管理展示（设备/IP/最近活跃，不含 token） */
    @Anonymous
    @GetMapping("/sessions")
    public AjaxResult sessions(jakarta.servlet.http.HttpServletRequest request)
    {
        String cardNo = readerSessionService.resolveFromRequest(request);
        if (cardNo == null)
        {
            return error("登录已失效，请重新登录");
        }
        return success(readerSessionService.listSessions(cardNo));
    }

    /** 退出其他设备（会话鉴权）：保留当前会话 */
    @Anonymous
    @PostMapping("/sessions/revoke-other")
    public AjaxResult revokeOther(jakarta.servlet.http.HttpServletRequest request)
    {
        String cardNo = readerSessionService.resolveFromRequest(request);
        if (cardNo == null)
        {
            return error("登录已失效，请重新登录");
        }
        String current = request.getHeader("X-Session-Token");
        int n = readerSessionService.revokeOthers(cardNo, current);
        return success("已退出 " + n + " 台其他设备");
    }

    /** 挂失补办：生成新证号并恢复状态（旧证号作废）
     * 注意：success(字符串) 会走 msg 字段，前端取 data 拿不到，必须显式放 data */
    @PreAuthorize("@ss.hasPermi('system:reader:edit')")
    @Log(title = "读者管理", businessType = BusinessType.UPDATE)
    @PutMapping("/reissue/{readerId}")
    public AjaxResult reissue(@PathVariable("readerId") Long readerId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", readerService.reissueCard(readerId));
        return ajax;
    }

    /** 前台补办报名证（匿名）：姓名+登记手机号校验 → 生成新证号（旧证号作废）
     * 频控：按 IP 维度（补办即换证号，防脚本批量作废他人证号） */
    @Anonymous
    @PostMapping("/applyReissue")
    public AjaxResult applyReissue(String readerName, String phone, jakarta.servlet.http.HttpServletRequest request)
    {
        if (readerName == null || readerName.trim().isEmpty() || phone == null || phone.trim().isEmpty())
        {
            return error("请输入姓名和登记手机号");
        }
        String failKey = "reissue:" + com.ruoyi.common.utils.ip.IpUtils.getIpAddr(request);
        if (readerSessionService.isBlocked(failKey))
        {
            return error("尝试次数过多，请 30 分钟后再试");
        }
        Reader nameQuery = new Reader();
        nameQuery.setReaderName(readerName.trim());
        java.util.List<Reader> list = readerService.selectReaderList(nameQuery);
        if (list == null || list.isEmpty())
        {
            readerSessionService.recordFail(failKey);
            return error("未找到该姓名的读者，请确认是否已登记");
        }
        // 手机号精确匹配（reader_name 是模糊查询，这里必须精确比对）
        Reader matched = null;
        for (Reader r : list)
        {
            if (phone.trim().equals(r.getPhone()))
            {
                matched = r;
                break;
            }
        }
        if (matched == null)
        {
            readerSessionService.recordFail(failKey);
            return error("手机号与登记信息不匹配");
        }
        readerSessionService.clearFail(failKey);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("data", readerService.reissueCard(matched.getReaderId()));
        return ajax;
    }

    /** 前台修改个人信息：短期成员会话 + 姓名/证号校验后更新手机号 */
    @Anonymous
    @PostMapping("/updateMyInfo")
    public AjaxResult updateMyInfo(String cardNo, String sessionToken, String readerName, String phone, String email, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        if (readerName == null || readerName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty())
        {
            return error("参数不完整");
        }
        if (!phone.trim().matches("\\d{11}"))
        {
            return error("手机号格式不正确（需 11 位数字）");
        }
        Reader query = new Reader();
        query.setCardNo(cardNo.trim());
        java.util.List<Reader> list = readerService.selectReaderList(query);
        if (list == null || list.isEmpty())
        {
            return error("借书证号不存在");
        }
        Reader r = list.get(0);
        if (!readerName.trim().equals(r.getReaderName()))
        {
            return error("姓名与借书证号不匹配");
        }
        // 邮箱不允许在此接口修改（安全：改邮箱必须走验证码流程 change-email）
        if (email != null && !email.trim().isEmpty() && !email.trim().equalsIgnoreCase(r.getEmail()))
        {
            return error("修改邮箱请使用个人主页的账号安全功能（需邮箱验证码）");
        }
        r.setPhone(phone.trim());
        return toAjax(readerService.updateReader(r));
    }

    /** 校验并规范化邮箱：格式合法返回去空格值，否则返回 null */
    private String trimEmail(String email)
    {
        if (email == null) { return null; }
        String em = email.trim();
        if (em.isEmpty() || !em.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$") || em.length() > 50)
        {
            return null;
        }
        return em;
    }

    /** 后台添加成员（需要权限） */
    @PreAuthorize("@ss.hasPermi('system:reader:add')")
    @Log(title = "读者管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Reader reader)
    {
        return toAjax(readerService.insertReader(reader));
    }

    /**
     * 修改成员管理
     */
    @PreAuthorize("@ss.hasPermi('system:reader:edit')")
    @Log(title = "读者管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Reader reader)
    {
        return toAjax(readerService.updateReader(reader));
    }

    /**
     * 删除成员管理
     */
    @PreAuthorize("@ss.hasPermi('system:reader:remove')")
    @Log(title = "读者管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{readerIds}")
    public AjaxResult remove(@PathVariable Long[] readerIds)
    {
        return toAjax(readerService.deleteReaderByReaderIds(readerIds));
    }

    /**
     * 已删除成员列表（后台回收站视图）：仅返回 del_flag='2' 的成员，供恢复/永久删除
     */
    @PreAuthorize("@ss.hasPermi('system:reader:remove')")
    @GetMapping("/deletedList")
    public TableDataInfo deletedList(Reader reader)
    {
        reader.setDelFlag("2");
        startPage();
        List<Reader> list = readerService.selectReaderList(reader);
        return getDataTable(list);
    }

    /**
     * 恢复已删除成员（两态软删除）
     */
    @PreAuthorize("@ss.hasPermi('system:reader:remove')")
    @Log(title = "读者管理", businessType = BusinessType.UPDATE)
    @PutMapping("/restore/{readerIds}")
    public AjaxResult restore(@PathVariable Long[] readerIds)
    {
        return toAjax(readerService.restoreReaderByReaderIds(readerIds));
    }

    /**
     * 永久删除成员（两态软删除）：物理删除，不可恢复
     */
    @PreAuthorize("@ss.hasPermi('system:reader:remove')")
    @Log(title = "读者管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge/{readerIds}")
    public AjaxResult purge(@PathVariable Long[] readerIds)
    {
        return toAjax(readerService.purgeReaderByReaderIds(readerIds));
    }
}
