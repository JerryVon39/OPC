package com.ruoyi.system.controller;

import java.util.List;
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
import com.ruoyi.system.domain.BookPurchaseReq;
import com.ruoyi.system.service.IBookPurchaseReqService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 服务入驻申请申请Controller
 *
 * 前台搜索无结果时提交"申请入驻申请"，后台处理（待处理/已处理/已拒绝）
 *
 * @author ruoyi
 * @date 2026-08-18
 */
@RestController
@RequestMapping("/system/purchase")
public class BookPurchaseReqController extends BaseController
{
    @Autowired
    private IBookPurchaseReqService bookPurchaseReqService;

    @Autowired
    private com.ruoyi.system.service.ReaderSessionService readerSessionService;

    @Autowired
    private com.ruoyi.system.service.IReaderService readerService;

    /**
     * 查询入驻申请申请列表
     */
    @PreAuthorize("@ss.hasPermi('system:purchase:list')")
    @GetMapping("/list")
    public TableDataInfo list(BookPurchaseReq bookPurchaseReq)
    {
        startPage();
        List<BookPurchaseReq> list = bookPurchaseReqService.selectBookPurchaseReqList(bookPurchaseReq);
        return getDataTable(list);
    }

    /**
     * 获取入驻申请申请详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:purchase:query')")
    @GetMapping(value = "/{reqId}")
    public AjaxResult getInfo(@PathVariable("reqId") Long reqId)
    {
        return success(bookPurchaseReqService.selectBookPurchaseReqByReqId(reqId));
    }

    /**
     * 前台匿名提交入驻申请申请（搜索无结果时的"申请入驻申请"按钮）
     * 频控：按 IP 维度（30 分钟窗口内失败 5 次拦截，防脚本刷表）
     * 登录态：带合法会话则自动绑定到成员账号（个人主页「我的入驻申请」可见）
     */
    @Anonymous
    @PostMapping("/apply")
    public AjaxResult apply(BookPurchaseReq bookPurchaseReq, jakarta.servlet.http.HttpServletRequest request)
    {
        String failKey = "apply:" + com.ruoyi.common.utils.ip.IpUtils.getIpAddr(request);
        if (readerSessionService.isBlocked(failKey))
        {
            return error("操作过于频繁，请稍后再试");
        }
        // 登录态绑定：会话合法时把申请挂到该成员名下（不强制登录，匿名仍可提交）
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard != null)
        {
            com.ruoyi.system.domain.Reader q = new com.ruoyi.system.domain.Reader();
            q.setCardNo(sessionCard);
            java.util.List<com.ruoyi.system.domain.Reader> readers = readerService.selectReaderList(q);
            if (readers != null && !readers.isEmpty())
            {
                bookPurchaseReq.setReaderId(readers.get(0).getReaderId());
            }
        }
        try
        {
            return toAjax(bookPurchaseReqService.applyPurchase(bookPurchaseReq));
        }
        catch (Exception e)
        {
            // 业务校验失败也计一次失败（防脚本反复探测），成功申请不计数
            readerSessionService.recordFail(failKey);
            throw e;
        }
    }

    /** 前台我的入驻申请（匿名）：短期成员会话 + 证号校验后返回本人申请列表 */
    @Anonymous
    @GetMapping("/myList")
    public AjaxResult myList(String cardNo, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        com.ruoyi.system.domain.Reader q = new com.ruoyi.system.domain.Reader();
        q.setCardNo(cardNo.trim());
        java.util.List<com.ruoyi.system.domain.Reader> readers = readerService.selectReaderList(q);
        if (readers == null || readers.isEmpty())
        {
            return error("成员不存在");
        }
        BookPurchaseReq query = new BookPurchaseReq();
        query.setReaderId(readers.get(0).getReaderId());
        return success(bookPurchaseReqService.selectBookPurchaseReqList(query));
    }

    /**
     * 处理入驻申请申请（标记已处理/已拒绝）
     */
    @PreAuthorize("@ss.hasPermi('system:purchase:edit')")
    @Log(title = "入驻申请", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BookPurchaseReq bookPurchaseReq)
    {
        return toAjax(bookPurchaseReqService.updateBookPurchaseReq(bookPurchaseReq));
    }

    /**
     * 删除入驻申请申请
     */
    @PreAuthorize("@ss.hasPermi('system:purchase:remove')")
    @Log(title = "入驻申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reqIds}")
    public AjaxResult remove(@PathVariable Long[] reqIds)
    {
        return toAjax(bookPurchaseReqService.deleteBookPurchaseReqByReqIds(reqIds));
    }
}