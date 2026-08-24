package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.service.IShopOrderService;
import com.ruoyi.system.service.ReaderSessionService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 报名订单Controller
 */
@RestController
@RequestMapping("/system/order")
public class ShopOrderController extends BaseController
{
    @Autowired
    private IShopOrderService shopOrderService;

    @Autowired
    private ReaderSessionService readerSessionService;

    /** 订单列表 */
    @PreAuthorize("@ss.hasPermi('system:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(ShopOrder shopOrder)
    {
        startPage();
        List<ShopOrder> list = shopOrderService.selectShopOrderList(shopOrder);
        return getDataTable(list);
    }

    /** 订单详情 */
    @PreAuthorize("@ss.hasPermi('system:order:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return success(shopOrderService.selectShopOrderByOrderId(orderId));
    }

    /** 前台报名：短期成员会话 + 证号兼容校验 */
    @Anonymous
    @PostMapping("/create")
    public AjaxResult create(String cardNo, String sessionToken, Long bookId, Long quantity, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(shopOrderService.createOrder(sessionCard, bookId, quantity));
    }

    /** 前台我的订单：短期成员会话查询 */
    @Anonymous
    @GetMapping("/queryByCard")
    public AjaxResult queryByCard(String cardNo, String sessionToken, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return success(shopOrderService.selectOrdersByCard(sessionCard));
    }

    /** 前台取消订单：短期成员会话 + 证号归属校验 */
    @Anonymous
    @PostMapping("/cancelByCard")
    public AjaxResult cancelByCard(String cardNo, String sessionToken, Long orderId, jakarta.servlet.http.HttpServletRequest request)
    {
        String sessionCard = readerSessionService.resolveFromRequest(request);
        if (sessionCard == null || cardNo == null || !sessionCard.equals(cardNo.trim()))
        {
            return error("登录已失效，请重新登录");
        }
        return toAjax(shopOrderService.cancelByCard(sessionCard, orderId));
    }

    /** 修改订单（状态流转） */
    @PreAuthorize("@ss.hasPermi('system:order:edit')")
    @Log(title = "报名订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ShopOrder shopOrder)
    {
        return toAjax(shopOrderService.updateShopOrder(shopOrder));
    }

    /** 删除订单 */
    @PreAuthorize("@ss.hasPermi('system:order:remove')")
    @Log(title = "报名订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(shopOrderService.deleteShopOrderByOrderIds(orderIds));
    }
}
