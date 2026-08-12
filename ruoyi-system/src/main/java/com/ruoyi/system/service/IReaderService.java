package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Reader;

/**
 * 读者管理Service接口
 * 
 * @author Jerry
 * @date 2026-08-12
 */
public interface IReaderService 
{
    /**
     * 查询读者管理
     * 
     * @param readerId 读者管理主键
     * @return 读者管理
     */
    public Reader selectReaderByReaderId(Long readerId);

    /**
     * 查询读者管理列表
     * 
     * @param reader 读者管理
     * @return 读者管理集合
     */
    public List<Reader> selectReaderList(Reader reader);

    /**
     * 新增读者管理
     * 
     * @param reader 读者管理
     * @return 结果
     */
    public int insertReader(Reader reader);

    /**
     * 修改读者管理
     * 
     * @param reader 读者管理
     * @return 结果
     */
    public int updateReader(Reader reader);

    /**
     * 批量删除读者管理
     * 
     * @param readerIds 需要删除的读者管理主键集合
     * @return 结果
     */
    public int deleteReaderByReaderIds(Long[] readerIds);

    /**
     * 删除读者管理信息
     * 
     * @param readerId 读者管理主键
     * @return 结果
     */
    public int deleteReaderByReaderId(Long readerId);
}
