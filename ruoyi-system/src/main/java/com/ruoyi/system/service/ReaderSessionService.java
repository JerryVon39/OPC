package com.ruoyi.system.service;

public interface ReaderSessionService
{
    String create(String cardNo);

    String resolve(String token);

    void remove(String token);

    /** 登录/补办频控：该 key（IP/证号维度）失败次数是否已达上限（拦截） */
    boolean isBlocked(String key);

    /** 记录一次失败（首次失败起 30 分钟窗口，窗口内叠加计数） */
    void recordFail(String key);

    /** 成功后清除失败计数 */
    void clearFail(String key);
}
