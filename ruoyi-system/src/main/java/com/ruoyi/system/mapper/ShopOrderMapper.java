package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ShopOrder;

public interface ShopOrderMapper
{
    public ShopOrder selectShopOrderByOrderId(Long orderId);

    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder);

    public int insertShopOrder(ShopOrder shopOrder);

    public int updateShopOrder(ShopOrder shopOrder);

    /** 仅当订单仍为指定状态时更新，返回实际更新行数 */
    public int updateStatusIfCurrent(@org.apache.ibatis.annotations.Param("orderId") Long orderId,
            @org.apache.ibatis.annotations.Param("fromStatus") String fromStatus,
            @org.apache.ibatis.annotations.Param("toStatus") String toStatus);

    public int deleteShopOrderByOrderId(Long orderId);

    public int updateCardNoSnapshot(@org.apache.ibatis.annotations.Param("readerId") Long readerId,
            @org.apache.ibatis.annotations.Param("newCardNo") String newCardNo);

    public int deleteShopOrderByOrderIds(Long[] orderIds);
}
