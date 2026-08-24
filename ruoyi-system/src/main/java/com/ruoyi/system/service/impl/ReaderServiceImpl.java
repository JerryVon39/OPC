package com.ruoyi.system.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.mapper.BookReserveMapper;
import com.ruoyi.system.mapper.BorrowRecordMapper;
import com.ruoyi.system.mapper.ShopOrderMapper;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.domain.BorrowRecord;
import com.ruoyi.system.domain.ShopOrder;
import com.ruoyi.system.service.IReaderService;
import com.ruoyi.system.service.StatisticsService;
import com.ruoyi.system.service.IMailTemplateService;

/**
 * 成员管理Service业务层处理
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
    private BookReserveMapper bookReserveMapper;

    @Autowired
    private ShopOrderMapper shopOrderMapper;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private com.ruoyi.system.service.ISysDictDataService sysDictDataService;

    @Autowired
    private IMailTemplateService mailTemplateService;

    /** 取当前操作人（未登录/定时任务场景返回空串，不抛异常） */
    private String operator()
    {
        try { return SecurityUtils.getUsername(); }
        catch (Exception e) { return ""; }
    }

    /**
     * 查询成员管理
     * 
     * @param readerId 成员管理主键
     * @return 成员管理
     */
    @Override
    public Reader selectReaderByReaderId(Long readerId)
    {
        return readerMapper.selectReaderByReaderId(readerId);
    }

    /**
     * 查询成员管理列表
     * 
     * @param reader 成员管理
     * @return 成员管理
     */
    @Override
    public List<Reader> selectReaderList(Reader reader)
    {
        return readerMapper.selectReaderList(reader);
    }

    /**
     * 新增成员管理
     *
     * @param reader 成员管理
     * @return 结果
     */
    @Override
    /** 前台自助登记：证号由后端生成（JS+8位随机数字），查重直到唯一，防止伪造/占坑
     * email 由 Controller 层完成必填/格式校验后传入，此处仅落库 */
    public Reader register(String readerName, String phone, String readerType, String email, String remark)
    {
        Reader reader = new Reader();
        reader.setReaderName(readerName);
        reader.setPhone(phone);
        reader.setEmail(email);
        reader.setReaderType(readerType);
        reader.setRemark(remark);
        reader.setStatus("0");
        reader.setCardNo(generateCardNo());
        insertReader(reader);
        return reader;
    }

    /** 证号即登录凭证，必须不可预测：SecureRandom 8 位随机数字（时间戳可预测——
     * 注册一个即可推算同窗口期他人证号），查重直到唯一 */
    private static final java.security.SecureRandom CARD_RANDOM = new java.security.SecureRandom();

    private String generateCardNo()
    {
        for (int i = 0; i < 10; i++)
        {
            String cardNo = "JS" + String.format("%08d", CARD_RANDOM.nextInt(100000000));
            if (readerMapper.countByCardNo(cardNo) == 0)
            {
                return cardNo;
            }
        }
        throw new com.ruoyi.common.exception.ServiceException("证号生成失败，请稍后重试");
    }

    public int insertReader(Reader reader)
    {
        // 证号唯一性校验：有证号的登记/添加必须先查重（含已删除软删行——软删记录仍占用 uk_card_no，防止裸数据库异常）
        if (reader.getCardNo() != null && !reader.getCardNo().trim().isEmpty())
        {
            String cardNo = reader.getCardNo().trim();
            if (readerMapper.countByCardNo(cardNo) > 0)
            {
                throw new com.ruoyi.common.exception.ServiceException("该借书证号已被使用，请更换");
            }
            reader.setCardNo(cardNo);
        }
        else
        {
            // 证号留空自动生成（后台添加成员零负担，与前台登记一致）
            reader.setCardNo(generateCardNo());
        }
        reader.setCreateTime(DateUtils.getNowDate());
        int rows = readerMapper.insertReader(reader);
        // 成员总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 修改成员管理
     * 
     * @param reader 成员管理
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
     * 按证号查询成员（不存在抛异常），供前台各接口复用。
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
     * 两步写（换证号 + 同步历史快照）放同一事务：换号成功而快照失败会丢"我的报名"关联 */
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
        // 同步历史报名快照证号（同一人换证号，历史记录归到新证号下，"我的报名"仍可查全）
        borrowRecordMapper.updateCardNoSnapshot(readerId, newCard);
        // 同步历史候补快照证号（同上；否则"我的候补"查不到、reserveByCard 重复候补校验失效、cancelByCard 归属比对对不上）
        bookReserveMapper.updateCardNoSnapshot(readerId, newCard);
        // 新证号邮件通知（模板渲染、异步、尽力而为）
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("readerName", reader.getReaderName());
        params.put("cardNo", newCard);
        mailTemplateService.send("reissue.notify", reader.getEmail(), params);
        return newCard;
    }

    /** 批量删除成员管理
     * READ_COMMITTED：加锁后对报名/订单的检查均读最新已提交数据（REPEATABLE READ 下批量第2个起
     * 的检查会读第1个建立的旧快照，漏看并发提交的报名/订单）
     *
     * @param readerIds 需要删除的成员管理主键
     * @return 结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int deleteReaderByReaderIds(Long[] readerIds)
    {
        // 排序：批量删除的加锁顺序一致，避免并发批量删除互相持锁等待（死锁）
        Arrays.sort(readerIds);
        for (Long readerId : readerIds)
        {
            // 锁成员行（FOR UPDATE）：防止检查通过后、删除前并发报名/下单/候补（检查与删除同事务原子化）
            Reader reader = readerMapper.selectReaderByReaderIdForUpdate(readerId);
            if (reader == null)
            {
                continue;
            }
            // 有未完成报名（进行中/逾期）的成员不可删
            BorrowRecord q = new BorrowRecord();
            q.setReaderId(readerId);
            List<BorrowRecord> records = borrowRecordMapper.selectBorrowRecordList(q);
            for (BorrowRecord r : records)
            {
                if ("0".equals(r.getStatus()) || "2".equals(r.getStatus()))
                {
                    throw new com.ruoyi.common.exception.ServiceException("该成员存在未完成的报名记录，无法删除");
                }
            }
            // 有待处理订单的成员不可删
            ShopOrder oq = new ShopOrder();
            oq.setReaderId(readerId);
            oq.setStatus("0");
            List<ShopOrder> orders = shopOrderMapper.selectShopOrderList(oq);
            if (orders != null && !orders.isEmpty())
            {
                throw new com.ruoyi.common.exception.ServiceException("该读者存在待处理订单，无法删除");
            }
            // 校验全部通过：进入软删除（数据保留在原表，标记 del_flag='2'）
        }
        // 软删除（两态）：成员对前台/列表不可见，后台提供「恢复」与「永久删除」；同事务，任一失败整体回滚
        int rows = readerMapper.softDeleteReaderByReaderIds(readerIds, operator(), new Date());
        // 成员总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 删除成员管理信息
     *
     * @param readerId 成员管理主键
     * @return 结果
     */
    @Override
    public int deleteReaderByReaderId(Long readerId)
    {
        // 统一走批量软删除（保证两态一致性）
        return deleteReaderByReaderIds(new Long[] { readerId });
    }

    /** 恢复已删除成员：del_flag 置 '0'，重新对前台/列表可见 */
    @Override
    @Transactional
    public int restoreReaderByReaderIds(Long[] readerIds)
    {
        return readerMapper.restoreReaderByReaderIds(readerIds);
    }

    /** 永久删除成员：物理删除，不可恢复 */
    @Override
    @Transactional
    public int purgeReaderByReaderIds(Long[] readerIds)
    {
        int rows = readerMapper.deleteReaderByReaderIds(readerIds);
        // 成员总数变了：失效统计缓存
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 批量导入成员：逐行校验（姓名必填/手机号格式/类型字典/证号判重），
     * 证号留空走 insertReader 自动生成；错误不中断整批，收集行号明细
     */
    @Override
    public java.util.Map<String, Object> importReaders(java.util.List<Reader> readers)
    {
        // 成员类型字典值集合（一次查询复用整批）
        java.util.Set<String> typeSet = new java.util.HashSet<>();
        com.ruoyi.common.core.domain.entity.SysDictData typeQuery = new com.ruoyi.common.core.domain.entity.SysDictData();
        typeQuery.setDictType("reader_type");
        for (com.ruoyi.common.core.domain.entity.SysDictData d : sysDictDataService.selectDictDataList(typeQuery))
        {
            typeSet.add(d.getDictValue());
        }
        int success = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        for (int i = 0; i < readers.size(); i++)
        {
            Reader r = readers.get(i);
            if (r == null)
            {
                continue; // Excel 尾部空行解析为 null，跳过
            }
            int row = i + 2; // 模板第 1 行为大标题、第 2 行为列名，数据从第 3 行起
            if (r.getReaderName() == null || r.getReaderName().trim().isEmpty())
            {
                errors.add("第" + row + "行：读者姓名不能为空");
                continue;
            }
            r.setReaderName(r.getReaderName().trim());
            if (r.getPhone() == null || !r.getPhone().trim().matches("\\d{11}"))
            {
                errors.add("第" + row + "行：读者" + r.getReaderName() + " 手机号格式不正确（需 11 位数字）");
                continue;
            }
            r.setPhone(r.getPhone().trim());
            // 新成员导入邮箱必填且格式合法（邮件通知前提）
            if (r.getEmail() == null || !r.getEmail().trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$") || r.getEmail().trim().length() > 50)
            {
                errors.add("第" + row + "行：读者" + r.getReaderName() + " 电子邮箱为空或格式不正确");
                continue;
            }
            r.setEmail(r.getEmail().trim());
            if (r.getReaderType() != null && !r.getReaderType().trim().isEmpty() && !typeSet.contains(r.getReaderType().trim()))
            {
                errors.add("第" + row + "行：读者" + r.getReaderName() + " 类型不在字典内");
                continue;
            }
            if (r.getCardNo() != null && !r.getCardNo().trim().isEmpty())
            {
                r.setCardNo(r.getCardNo().trim());
                if (readerMapper.countByCardNo(r.getCardNo()) > 0)
                {
                    errors.add("第" + row + "行：读者" + r.getReaderName() + " 证号 " + r.getCardNo() + " 已存在，已跳过");
                    continue;
                }
            }
            r.setStatus(r.getStatus() == null || r.getStatus().trim().isEmpty() ? "0" : r.getStatus());
            try
            {
                insertReader(r);
                success++;
            }
            catch (Exception ex)
            {
                // 行级异常（如超长字段等数据库约束）：不中断整批，收集行号明细（此前行保持已导入）
                errors.add("第" + row + "行：读者" + r.getReaderName() + " 保存失败，请检查数据后重试");
            }
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", success);
        result.put("fail", errors.size());
        result.put("errors", errors);
        return result;
    }
}
