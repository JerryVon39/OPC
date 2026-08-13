package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Override
    public int updateShopOrder(ShopOrder shopOrder)
    {
        return shopOrderMapper.updateShopOrder(shopOrder);
    }

    @Override
    public int deleteShopOrderByOrderIds(Long[] orderIds)
    {
        return shopOrderMapper.deleteShopOrderByOrderIds(orderIds);
    }

    /** 前台购书：校验读者/图书/库存 → 创建订单 → 库存-1 */
    @Override
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
        // 库存-1（购买=售出）
        book.setStock(book.getStock() - quantity);
        bookMapper.updateBook(book);
        return shopOrderMapper.insertShopOrder(order);
    }

    @Override
    public List<ShopOrder> selectOrdersByCard(String cardNo)
    {
        ShopOrder query = new ShopOrder();
        query.setCardNo(cardNo);
        return shopOrderMapper.selectShopOrderList(query);
    }
}
