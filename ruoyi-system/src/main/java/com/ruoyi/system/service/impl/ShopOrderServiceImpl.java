package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.constant.BizStatus;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.service.IShopOrderService;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.StatisticsService;

@Service
public class ShopOrderServiceImpl implements IShopOrderService
{
    @Autowired
    private ShopOrderMapper shopOrderMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @Autowired
    private IReaderService readerService;

    @Autowired
    private StatisticsService statisticsService;

    @Override
    public ShopOrder selectShopOrderByOrderId(Long orderId)
    {
        return shopOrderMapper.selectShopOrderByOrderId(orderId);
    }

    @Override
    public List<ShopOrder> selectShopOrderList(ShopOrder shopOrder)
    {
        return shopOrderMapper.selectShopOrderList(shopOrder);
    }

    @Override
    public int insertShopOrder(ShopOrder shopOrder)
    {
        return shopOrderMapper.insertShopOrder(shopOrder);
    }

    /** 修改订单：状态机校验 + 流转
     * 允许：0待付款 → 1完成 / 2取消 / 3已收款；3已收款 → 1完成。
     * 其余流转（含已取消/已完成订单再改单）一律拒绝，防止"库存已回补却仍被标记卖出"等账实不符。
     * 仅"待付款 → 取消"回滚库存（与前台 cancelByCard 语义一致）；订单不存在抛明确异常。 */
    @Override
    @Transactional
    public int updateShopOrder(ShopOrder shopOrder)
    {
        if (shopOrder.getOrderId() == null || shopOrder.getStatus() == null)
        {
            throw new ServiceException("参数不完整");
        }
        ShopOrder old = shopOrderMapper.selectShopOrderByOrderId(shopOrder.getOrderId());
        if (old == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (!isOrderTransitionAllowed(old.getStatus(), shopOrder.getStatus()))
        {
            throw new ServiceException("不允许从" + orderStatusText(old.getStatus()) + "变更为" + orderStatusText(shopOrder.getStatus()));
        }
        // 待付款 → 取消：CAS 原子转换（与前台 cancelByCard 口径一致），只有抢到转换权的请求回补库存，
        // 防止两个管理员同时取消同一订单导致库存双回补
        if (BizStatus.ORDER_CANCELLED.equals(shopOrder.getStatus()))
        {
            int rows = shopOrderMapper.updateStatusIfCurrent(shopOrder.getOrderId(),
                    BizStatus.ORDER_UNPAID, BizStatus.ORDER_CANCELLED);
            if (rows == 0)
            {
                throw new ServiceException("该订单状态已变化，请刷新后重试");
            }
            if (old.getBookId() != null && old.getQuantity() != null)
            {
                bookMapper.restoreStock(old.getBookId(), old.getQuantity());
            }
            return rows;
        }
        return shopOrderMapper.updateShopOrder(shopOrder);
    }

    /** 订单状态机：允许的流转（0待付款/1完成/2取消/3已收款） */
    private boolean isOrderTransitionAllowed(String from, String to)
    {
        if (BizStatus.ORDER_UNPAID.equals(from))
        {
            return BizStatus.ORDER_COMPLETED.equals(to) || BizStatus.ORDER_CANCELLED.equals(to) || BizStatus.ORDER_PAID.equals(to);
        }
        if (BizStatus.ORDER_PAID.equals(from))
        {
            return BizStatus.ORDER_COMPLETED.equals(to);
        }
        return false;
    }

    private String orderStatusText(String status)
    {
        if (BizStatus.ORDER_UNPAID.equals(status)) return "待付款";
        if (BizStatus.ORDER_COMPLETED.equals(status)) return "已完成";
        if (BizStatus.ORDER_CANCELLED.equals(status)) return "已取消";
        if (BizStatus.ORDER_PAID.equals(status)) return "已收款";
        return "未知状态";
    }

    /** 删除订单：待付款订单删除 = 硬取消，先还原库存（避免书被"吃掉"）；其余状态直接删 */
    @Override
    @Transactional
    public int deleteShopOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            ShopOrder order = shopOrderMapper.selectShopOrderByOrderId(orderId);
            if (order == null)
            {
                continue;
            }
            if (BizStatus.ORDER_UNPAID.equals(order.getStatus()))
            {
                // CAS 先置"已取消"再删：并发删除同一待付款订单时只有一次回补成功（防库存双回补）
                int locked = shopOrderMapper.updateStatusIfCurrent(orderId,
                        BizStatus.ORDER_UNPAID, BizStatus.ORDER_CANCELLED);
                if (locked == 0)
                {
                    continue; // 已被并发处理，跳过不再回补
                }
                if (order.getBookId() != null && order.getQuantity() != null)
                {
                    bookMapper.restoreStock(order.getBookId(), order.getQuantity());
                }
            }
        }
        int rows = shopOrderMapper.deleteShopOrderByOrderIds(orderIds);
        // 订单数据变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /** 前台购书：校验读者/图书/库存 → 创建订单 → 库存-1（事务：下单与扣库存同生共死） */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int createOrder(String cardNo, Long bookId, Long quantity)
    {
        if (cardNo == null || cardNo.trim().isEmpty())
        {
            throw new ServiceException("请先登录");
        }
        if (quantity == null || quantity < 1)
        {
            quantity = 1L;
        }
        // 锁读者行（FOR UPDATE）：与删除读者互斥，防止"删除检查通过后、下单插入订单"的竞态
        Reader queryReader = readerService.findActiveReader(cardNo);
        Reader reader = readerMapper.selectReaderByReaderIdForUpdate(queryReader.getReaderId());
        // findActiveReader 与加锁之间读者可能被管理端删除：加锁查不到即视为不存在
        if (reader == null)
        {
            throw new ServiceException("读者不存在");
        }
        if (!com.ruoyi.system.constant.BizStatus.READER_NORMAL.equals(reader.getStatus()))
        {
            throw new ServiceException("该读者证号已停用/挂失，无法购买");
        }
        // 锁图书行（FOR UPDATE，加锁顺序统一为 读者→图书，与借书/预约路径一致）：
        // 与下架（changeBookStatus）/删除图书共享 book 行锁，下架完成后本事务读到的必是最新状态，
        // 已下架的书在此被拦截（"该图书已下架，无法购买"），下架与新下单因此串行化
        Book book = bookMapper.selectBookByBookIdForUpdate(bookId);
        if (book == null)
        {
            throw new ServiceException("图书不存在");
        }
        if (!"0".equals(book.getStatus()))
        {
            throw new ServiceException("该图书已下架，无法购买");
        }
        if (book.getStock() == null || book.getStock() < quantity)
        {
            throw new ServiceException("库存不足，无法购买");
        }
        // 创建订单（订单号 = 毫秒时间戳 + 3 位随机后缀：order_no 有唯一索引，防同毫秒并发撞约束）
        ShopOrder order = new ShopOrder();
        order.setOrderNo("WSW" + System.currentTimeMillis()
                + String.format("%03d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1000)));
        order.setReaderId(reader.getReaderId());
        order.setReaderName(reader.getReaderName());
        order.setCardNo(reader.getCardNo());
        order.setBookId(book.getBookId());
        order.setBookName(book.getBookName());
        order.setQuantity(quantity);
        order.setTotalPrice(book.getPrice() == null ? BigDecimal.ZERO : book.getPrice().multiply(new BigDecimal(quantity)));
        order.setStatus("0");
        order.setCreateBy(reader.getReaderName());
        order.setCreateTime(new Date());
        // 库存-1（购买=售出）：原子条件更新，并发下不超卖（0行=期间库存被抢光）
        if (bookMapper.updateStock(book.getBookId(), quantity) == 0)
        {
            throw new ServiceException("库存不足，无法购买");
        }
        int rows = shopOrderMapper.insertShopOrder(order);
        // 订单数据变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public List<ShopOrder> selectOrdersByCard(String cardNo)
    {
        ShopOrder query = new ShopOrder();
        query.setCardNo(cardNo);
        return shopOrderMapper.selectShopOrderList(query);
    }

    /** 前台取消订单：证号归属校验 + 仅"待处理"可取消 + 回滚库存
     * 注意：直接用 mapper 更新，不走 updateShopOrder（避免二次回补库存） */
    @Override
    @Transactional
    public int cancelByCard(String cardNo, Long orderId)
    {
        if (cardNo == null || cardNo.trim().isEmpty() || orderId == null)
        {
            throw new ServiceException("参数不完整");
        }
        ShopOrder order = shopOrderMapper.selectShopOrderByOrderId(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (!cardNo.trim().equals(order.getCardNo()))
        {
            throw new ServiceException("该订单不属于此证号");
        }
        // 先原子完成取消状态转换，只有成功请求才能回补库存，避免并发双回补
        int rows = shopOrderMapper.updateStatusIfCurrent(orderId, BizStatus.ORDER_UNPAID, BizStatus.ORDER_CANCELLED);
        if (rows == 0)
        {
            throw new ServiceException("该订单已处理，无法取消");
        }
        // 回滚库存（仅状态转换成功后回补）
        if (order.getBookId() != null && order.getQuantity() != null)
        {
            bookMapper.restoreStock(order.getBookId(), order.getQuantity());
        }
        // 订单数据变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }
}
