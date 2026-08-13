package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.ShopOrder;

public interface IShopOrderService
{
    public ShopOrder selectShopOrderByOrderId(Long orderId);

    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder);

    public int insertShopOrder(ShopOrder shopOrder);

    public int updateShopOrder(ShopOrder shopOrder);

    public int deleteShopOrderByOrderIds(Long[] orderIds);

    /** 前台购书：按证号下单（校验+库存-1） */
    public int createOrder(String cardNo, Long bookId, Long quantity);

    /** 按证号查询订单（我的订单） */
    public List<ShopOrder> selectOrdersByCard(String cardNo);

    /** 前台取消订单：证号归属校验 + 仅待处理可取消 + 回滚库存 */
    public int cancelByCard(String cardNo, Long orderId);
}
