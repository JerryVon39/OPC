package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Reader;

/**
 * 成员管理Mapper接口
 * 
 * @author Jerry
 * @date 2026-08-12
 */
public interface ReaderMapper 
{
    /**
     * 查询成员管理
     * 
     * @param readerId 成员管理主键
     * @return 成员管理
     */
    public Reader selectReaderByReaderId(Long readerId);

    /** 行锁查询（FOR UPDATE）：并发写路径（报名/预约/下单/删除成员）串行化同一成员的操作 */
    public Reader selectReaderByReaderIdForUpdate(Long readerId);

    /** 批量导入判重：成员编号是否已存在（uk_card_no 冲突前先友好提示） */
    public int countByCardNo(@Param("cardNo") String cardNo);

    /** 邮箱判重：改邮箱/注册前友好提示（uk_email 兜底） */
    public int countByEmail(@Param("email") String email);

    public int countByPhone(@Param("phone") String phone);

    /**
     * 查询成员管理列表
     * 
     * @param reader 成员管理
     * @return 成员管理集合
     */
    public List<Reader> selectReaderList(Reader reader);

    /**
     * 新增成员管理
     * 
     * @param reader 成员管理
     * @return 结果
     */
    public int insertReader(Reader reader);

    /**
     * 修改成员管理
     * 
     * @param reader 成员管理
     * @return 结果
     */
    public int updateReader(Reader reader);

    /**
     * 删除成员管理
     * 
     * @param readerId 成员管理主键
     * @return 结果
     */
    public int deleteReaderByReaderId(Long readerId);

    /**
     * 批量删除成员管理
     *
     * @param readerIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteReaderByReaderIds(Long[] readerIds);

    /** 软删除（回收站两态）：del_flag 置 '2'，记录删除人/时间；仅对未删除行生效（幂等） */
    public int softDeleteReaderByReaderIds(@Param("readerIds") Long[] readerIds,
            @Param("deletedBy") String deletedBy, @Param("deletedTime") java.util.Date deletedTime);

    /** 恢复已删除成员：del_flag 置 '0'，清空删除人/时间；仅对已删除行生效（幂等） */
    public int restoreReaderByReaderIds(@Param("readerIds") Long[] readerIds);

    /** 按登录标识查认证信息（证号/手机号/邮箱，含 password_hash/pwd_set/email_verified，
     * 登录/改密/重置专用；普通查询不含密码哈希，防止密码列泄露到列表/前台接口） */
    public Reader selectAuthInfo(@Param("account") String account);

    /** 更新认证字段（password_hash/pwd_set/email_verified/phone_verified/last_login_time） */
    public int updateAuth(Reader reader);

    /** 修改邮箱（email + email_verified 重置为未验证） */
    public int updateEmail(Reader reader);
}
