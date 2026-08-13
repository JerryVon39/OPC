package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ShopOrder;

public interface ShopOrderMapper
{
    public ShopOrder selectShopOrderByOrderId(Long orderId);

    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder);

    public int insertShopOrder(ShopOrder shopOrder);

    public int updateShopOrder(ShopOrder shopOrder);

    public int deleteShopOrderByOrderId(Long orderId);

    public int deleteShopOrderByOrderIds(Long[] orderIds);
}
