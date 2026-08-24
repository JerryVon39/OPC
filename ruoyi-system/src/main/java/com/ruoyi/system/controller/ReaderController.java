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
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.ReaderSessionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

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
    /** 前台成员登录（匿名）：姓名+成员编号验证
     * 注意：按证号精确查询后精确比对姓名——不能用 LIKE 模糊匹配姓名（知道证号即可猜登录）
     * 证号即登录凭证：错误提示统一为"姓名或成员编号不正确"（不暴露证号是否存在，防枚举），
     * 并按 IP+证号 维度频控（30 分钟窗口内失败 5 次拦截，防爆破） */
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(String readerName, String cardNo, jakarta.servlet.http.HttpServletRequest request)
    {
        if (readerName == null || readerName.trim().isEmpty() || cardNo == null || cardNo.trim().isEmpty())
        {
            return error("请输入姓名和借书证号");
        }
        String failKey = "login:" + com.ruoyi.common.utils.ip.IpUtils.getIpAddr(request) + ":" + cardNo.trim();
        if (readerSessionService.isBlocked(failKey))
        {
            return error("尝试次数过多，请 30 分钟后再试");
        }
        Reader query = new Reader();
        query.setCardNo(cardNo.trim());
        java.util.List<Reader> list = readerService.selectReaderList(query);
        if (list == null || list.isEmpty())
        {
            readerSessionService.recordFail(failKey);
            return error("姓名或借书证号不正确");
        }
        Reader r = list.get(0);
        if (!readerName.trim().equals(r.getReaderName()))
        {
            readerSessionService.recordFail(failKey);
            return error("姓名或借书证号不正确");
        }
        // 停用/挂失的证号不允许登录前台（报名、下单前就把问题拦下）
        if (!"0".equals(r.getStatus()))
        {
            return error("该借书证号已停用/挂失，请联系管理员");
        }
        readerSessionService.clearFail(failKey);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("readerId", r.getReaderId());
        result.put("readerName", r.getReaderName());
        result.put("cardNo", r.getCardNo());
        result.put("readerType", r.getReaderType());
        result.put("status", r.getStatus());
        result.put("sessionToken", readerSessionService.create(r.getCardNo()));
        return success(result);
    }

    /** 前台自助登记（匿名）：证号由后端生成，防止客户端伪造/占用证号
     * 频控：按 IP 维度（30 分钟窗口内失败 5 次拦截，防脚本刷库灌垃圾成员） */
    @Anonymous
    @PostMapping("/register")
    public AjaxResult register(String readerName, String phone, String readerType, String email, String remark,
            jakarta.servlet.http.HttpServletRequest request)
    {
        String failKey = "register:" + com.ruoyi.common.utils.ip.IpUtils.getIpAddr(request);
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
        readerSessionService.clearFail(failKey);
        Reader r = readerService.register(readerName.trim(), phone.trim(), readerType.trim(), em, remark);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("readerId", r.getReaderId());
        result.put("readerName", r.getReaderName());
        result.put("cardNo", r.getCardNo());
        result.put("email", r.getEmail());
        // 注册即登录：直接签发会话令牌，前端免二次登录（与 login 返回结构一致）
        result.put("sessionToken", readerSessionService.create(r.getCardNo()));
        return success(result);
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
        // 邮箱允许修改：不传则保留原值；传了则必填且须格式合法
        String em = email == null ? r.getEmail() : trimEmail(email);
        if (em == null)
        {
            return error("请填写有效的电子邮箱（用于接收报名/候补通知）");
        }
        r.setPhone(phone.trim());
        r.setEmail(em);
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
}
