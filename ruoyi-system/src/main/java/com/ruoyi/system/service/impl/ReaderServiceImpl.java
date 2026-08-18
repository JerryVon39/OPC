package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.StatisticsService;

/**
 * 读者管理Service业务层处理
 * 
 * @author Jerry
 * @date 2026-08-12
 */
@Service
public class ReaderServiceImpl implements IReaderService 
{
    @Autowired
    private ReaderMapper readerMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private ShopOrderMapper shopOrderMapper;

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 查询读者管理
     * 
     * @param readerId 读者管理主键
     * @return 读者管理
     */
    @Override
    public Reader selectReaderByReaderId(Long readerId)
    {
        return readerMapper.selectReaderByReaderId(readerId);
    }

    /**
     * 查询读者管理列表
     * 
     * @param reader 读者管理
     * @return 读者管理
     */
    @Override
    public List<Reader> selectReaderList(Reader reader)
    {
        return readerMapper.selectReaderList(reader);
    }

    /**
     * 新增读者管理
     * 
     * @param reader 读者管理
     * @return 结果
     */
    @Override
    /** 前台自助登记：证号由后端生成（JS+时间戳后8位），查重直到唯一，防止伪造/占坑 */
    public Reader register(String readerName, String phone, String readerType, String remark)
    {
        Reader reader = new Reader();
        reader.setReaderName(readerName);
        reader.setPhone(phone);
        reader.setReaderType(readerType);
        reader.setRemark(remark);
        reader.setStatus("0");
        reader.setCardNo(generateCardNo());
        insertReader(reader);
        return reader;
    }

    /** 生成唯一证号：JS + 时间戳后8位，查重直到唯一 */
    private String generateCardNo()
    {
        for (int i = 0; i < 10; i++)
        {
            String cardNo = "JS" + String.valueOf(System.currentTimeMillis()).substring(5);
            Reader query = new Reader();
            query.setCardNo(cardNo);
            List<Reader> exists = readerMapper.selectReaderList(query);
            if (exists == null || exists.isEmpty())
            {
                return cardNo;
            }
        }
        throw new com.ruoyi.common.exception.ServiceException("证号生成失败，请稍后重试");
    }

    public int insertReader(Reader reader)
    {
        // 证号唯一性校验：有证号的登记/添加必须先查重（防止同证号多条记录）
        if (reader.getCardNo() != null && !reader.getCardNo().trim().isEmpty())
        {
            Reader query = new Reader();
            query.setCardNo(reader.getCardNo().trim());
            List<Reader> exists = readerMapper.selectReaderList(query);
            if (exists != null && !exists.isEmpty())
            {
                throw new com.ruoyi.common.exception.ServiceException("该借书证号已被使用，请更换");
            }
            reader.setCardNo(reader.getCardNo().trim());
        }
        else
        {
            // 证号留空自动生成（后台添加读者零负担，与前台登记一致）
            reader.setCardNo(generateCardNo());
        }
        reader.setCreateTime(DateUtils.getNowDate());
        int rows = readerMapper.insertReader(reader);
        // 读者总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 修改读者管理
     * 
     * @param reader 读者管理
     * @return 结果
     */
    @Override
    public int updateReader(Reader reader)
    {
        // 修改证号同样要查重（排除自身）：防撞 uk_card_no 唯一约束变成裸数据库异常
        if (reader.getCardNo() != null && !reader.getCardNo().trim().isEmpty())
        {
            String cardNo = reader.getCardNo().trim();
            Reader query = new Reader();
            query.setCardNo(cardNo);
            List<Reader> exists = readerMapper.selectReaderList(query);
            if (exists != null)
            {
                for (Reader r : exists)
                {
                    if (r.getReaderId() == null || !r.getReaderId().equals(reader.getReaderId()))
                    {
                        throw new com.ruoyi.common.exception.ServiceException("该借书证号已被使用，请更换");
                    }
                }
            }
            reader.setCardNo(cardNo);
        }
        reader.setUpdateTime(DateUtils.getNowDate());
        return readerMapper.updateReader(reader);
    }

    /**
     * 按证号查询读者（不存在抛异常），供前台各接口复用。
     * 注意：仅保证"存在"，不校验状态——停用/挂失由调用方按需拦截（见 createOrder/reserveByCard）。
     */
    @Override
    public Reader findActiveReader(String cardNo)
    {
        Reader query = new Reader();
        query.setCardNo(cardNo.trim());
        List<Reader> readers = readerMapper.selectReaderList(query);
        if (readers == null || readers.isEmpty())
        {
            throw new com.ruoyi.common.exception.ServiceException("借书证号不存在，请先登记");
        }
        return readers.get(0);
    }

    /** 挂失补办：生成新证号 + 状态恢复正常（旧证号作废，历史记录快照保留）
     * 两步写（换证号 + 同步历史快照）放同一事务：换号成功而快照失败会丢"我的借阅"关联 */
    @Override
    @Transactional
    public String reissueCard(Long readerId)
    {
        Reader reader = readerMapper.selectReaderByReaderId(readerId);
        if (reader == null)
        {
            throw new com.ruoyi.common.exception.ServiceException("读者不存在");
        }
        String newCard = generateCardNo();
        reader.setCardNo(newCard);
        reader.setStatus("0");
        reader.setUpdateTime(DateUtils.getNowDate());
        readerMapper.updateReader(reader);
        // 同步历史借阅快照证号（同一人换证号，历史记录归到新证号下，"我的借阅"仍可查全）
        borrowRecordMapper.updateCardNoSnapshot(readerId, newCard);
        return newCard;
    }

    /**
     * 批量删除读者管理
     *
     * @param readerIds 需要删除的读者管理主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteReaderByReaderIds(Long[] readerIds)
    {
        for (Long readerId : readerIds)
        {
            // 锁读者行（FOR UPDATE）：防止检查通过后、删除前并发借书/下单/预约（检查与删除同事务原子化）
            if (readerMapper.selectReaderByReaderIdForUpdate(readerId) == null)
            {
                continue;
            }
            // 有未归还借阅（借出中/逾期）的读者不可删
            BorrowRecord q = new BorrowRecord();
            q.setReaderId(readerId);
            List<BorrowRecord> records = borrowRecordMapper.selectBorrowRecordList(q);
            for (BorrowRecord r : records)
            {
                if ("0".equals(r.getStatus()) || "2".equals(r.getStatus()))
                {
                    throw new com.ruoyi.common.exception.ServiceException("该读者存在未归还的借阅记录，无法删除");
                }
            }
            // 有待处理订单的读者不可删
            ShopOrder oq = new ShopOrder();
            oq.setReaderId(readerId);
            oq.setStatus("0");
            List<ShopOrder> orders = shopOrderMapper.selectShopOrderList(oq);
            if (orders != null && !orders.isEmpty())
            {
                throw new com.ruoyi.common.exception.ServiceException("该读者存在待处理订单，无法删除");
            }
        }
        int rows = readerMapper.deleteReaderByReaderIds(readerIds);
        // 读者总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 删除读者管理信息
     * 
     * @param readerId 读者管理主键
     * @return 结果
     */
    @Override
    public int deleteReaderByReaderId(Long readerId)
    {
        return readerMapper.deleteReaderByReaderId(readerId);
    }
}
