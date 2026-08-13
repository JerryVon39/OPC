package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.ReaderMapper;
import com.ruoyi.system.domain.Reader;
import com.ruoyi.system.service.IReaderService;

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
        reader.setCreateTime(DateUtils.getNowDate());
        return readerMapper.insertReader(reader);
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
        reader.setUpdateTime(DateUtils.getNowDate());
        return readerMapper.updateReader(reader);
    }

    /**
     * 批量删除读者管理
     * 
     * @param readerIds 需要删除的读者管理主键
     * @return 结果
     */
    @Override
    public int deleteReaderByReaderIds(Long[] readerIds)
    {
        return readerMapper.deleteReaderByReaderIds(readerIds);
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
