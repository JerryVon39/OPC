package com.ruoyi.system.service;

import java.util.List;

import com.ruoyi.system.domain.ReaderLoginLog;

/**
 * 成员端登录/安全事件审计服务（尽力而为：写库失败不抛异常，不影响主业务）
 */
public interface IReaderLoginLogService
{
    /**
     * 记录审计事件
     *
     * @param cardNo 成员编号（失败时尽力记录）
     * @param readerId 成员ID（可空）
     * @param ip 来源 IP
     * @param event 事件：login/login_fail/logout/change_pwd/reset_pwd/change_email/register
     * @param success 是否成功
     * @param msg 备注
     */
    void log(String cardNo, Long readerId, String ip, String event, boolean success, String msg);

    /** 后台分页查询 */
    List<ReaderLoginLog> list(ReaderLoginLog query);
}
