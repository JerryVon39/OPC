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
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 购书订单Controller
 */
@RestController
@RequestMapping("/system/order")
public class ShopOrderController extends BaseController
{
    @Autowired
    private IShopOrderService shopOrderService;

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

    /** 前台购书（匿名）：按证号下单 */
    @Anonymous
    @PostMapping("/create")
    public AjaxResult create(String cardNo, Long bookId, Long quantity)
    {
        return toAjax(shopOrderService.createOrder(cardNo, bookId, quantity));
    }

    /** 前台我的订单（匿名）：按证号查询 */
    @Anonymous
    @GetMapping("/queryByCard")
    public AjaxResult queryByCard(String cardNo)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            return error("请输入借书证号");
        }
        return success(shopOrderService.selectOrdersByCard(cardNo.trim()));
    }

    /** 修改订单（状态流转） */
    @PreAuthorize("@ss.hasPermi('system:order:edit')")
    @Log(title = "购书订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ShopOrder shopOrder)
    {
        return toAjax(shopOrderService.updateShopOrder(shopOrder));
    }

    /** 删除订单 */
    @PreAuthorize("@ss.hasPermi('system:order:remove')")
    @Log(title = "购书订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(shopOrderService.deleteShopOrderByOrderIds(orderIds));
    }
}
