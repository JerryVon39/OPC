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

    /** 前台自助登记（第一步：资料+密码）：证号后端生成（防伪造/占坑），
     * 密码 BCrypt 加密落库（pwd_set=1、email_verified=0，随后需邮箱验证码完成注册） */
    public Reader register(String readerName, String phone, String readerType, String email, String remark, String password);

    /** 按证号查认证信息（含密码哈希，登录/改密/重置专用） */
    public Reader findAuthByCardNo(String cardNo);

    /** 设置/重置密码（BCrypt；pwd_set 置 1） */
    public int setPassword(String cardNo, String newPassword);

    /** 管理员直接设置密码（按成员 ID；忘记密码/代客设密场景） */
    public int setPasswordByReaderId(Long readerId, String newPassword);

    /** 修改密码：校验旧密码正确后更新（失败抛异常） */
    public int changePassword(String cardNo, String oldPassword, String newPassword);

    /** 邮箱验证通过：email_verified 置 1 */
    public int verifyEmail(String cardNo);

    /** 修改邮箱（新邮箱唯一性校验；调用方需先完成新邮箱验证码校验） */
    public int changeEmail(String cardNo, String newEmail);

    /** 记录登录时间（登录成功后调用） */
    public int touchLogin(Long readerId);

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

    /** 恢复已删除读者（两态）：del_flag 置 '0'，重新对前台/列表可见 */
    public int restoreReaderByReaderIds(Long[] readerIds);

    /** 永久删除读者（两态）：物理删除，不可恢复 */
    public int purgeReaderByReaderIds(Long[] readerIds);

    /**
     * 批量导入读者（Excel 逐行校验：姓名必填/手机号格式/类型字典/证号判重，
     * 证号留空自动生成；错误收集行号明细）
     *
     * @param readers 解析出的读者列表
     * @return {success: 成功条数, fail: 失败条数, errors: [行号+原因]}
     */
    public java.util.Map<String, Object> importReaders(java.util.List<Reader> readers);
}
