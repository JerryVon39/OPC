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

    /** 前台自助登记：证号后端生成（防伪造/占坑） */
    public Reader register(String readerName, String phone, String readerType, String remark);

    /** 挂失补办：生成新证号并恢复状态，返回新证号 */
    public String reissueCard(Long readerId);

    /** 按证号查询读者（不存在抛异常），供前台各接口复用 */
    public Reader findActiveReader(String cardNo);

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

    /**
     * 批量导入读者（Excel 逐行校验：姓名必填/手机号格式/类型字典/证号判重，
     * 证号留空自动生成；错误收集行号明细）
     *
     * @param readers 解析出的读者列表
     * @return {success: 成功条数, fail: 失败条数, errors: [行号+原因]}
     */
    public java.util.Map<String, Object> importReaders(java.util.List<Reader> readers);
}
