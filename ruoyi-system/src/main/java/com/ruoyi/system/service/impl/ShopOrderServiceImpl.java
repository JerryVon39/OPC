package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.mapper.BookMapper;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.service.IShopOrderService;

@Service
public class ShopOrderServiceImpl implements IShopOrderService
{
    @Autowired
    private ShopOrderMapper shopOrderMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ReaderMapper readerMapper;

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

    /** 修改订单：状态变为"已取消"时自动回滚库存（防重复取消双倍回补；已完成订单不可取消） */
    @Override
    @Transactional
    public int updateShopOrder(ShopOrder shopOrder)
    {
        if ("2".equals(shopOrder.getStatus()))
        {
            ShopOrder old = shopOrderMapper.selectShopOrderByOrderId(shopOrder.getOrderId());
            if (old != null)
            {
                if ("1".equals(old.getStatus()))
                {
                    throw new ServiceException("该订单已完成，不可取消");
                }
                if (!"2".equals(old.getStatus()))
                {
                    if (old.getBookId() != null && old.getQuantity() != null)
                    {
                        bookMapper.restoreStock(old.getBookId(), old.getQuantity());
                    }
                }
            }
        }
        return shopOrderMapper.updateShopOrder(shopOrder);
    }

    @Override
    public int deleteShopOrderByOrderIds(Long[] orderIds)
    {
        return shopOrderMapper.deleteShopOrderByOrderIds(orderIds);
    }

    /** 前台购书：校验读者/图书/库存 → 创建订单 → 库存-1（事务：下单与扣库存同生共死） */
    @Override
    @Transactional
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
        Reader query = new Reader();
        query.setCardNo(cardNo.trim());
        List<Reader> readers = readerMapper.selectReaderList(query);
        if (readers == null || readers.isEmpty())
        {
            throw new ServiceException("借书证号不存在，请先登记");
        }
        Reader reader = readers.get(0);
        if (!"0".equals(reader.getStatus()))
        {
            throw new ServiceException("该读者证号已停用/挂失，无法购买");
        }
        Book book = bookMapper.selectBookByBookId(bookId);
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
        // 创建订单
        ShopOrder order = new ShopOrder();
        order.setOrderNo("WSW" + System.currentTimeMillis());
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
        return shopOrderMapper.insertShopOrder(order);
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
        if (!"0".equals(order.getStatus()))
        {
            throw new ServiceException("该订单已处理，无法取消");
        }
        // 回滚库存（原子回补）
        if (order.getBookId() != null && order.getQuantity() != null)
        {
            bookMapper.restoreStock(order.getBookId(), order.getQuantity());
        }
        order.setStatus("2");
        return shopOrderMapper.updateShopOrder(order);
    }
}
