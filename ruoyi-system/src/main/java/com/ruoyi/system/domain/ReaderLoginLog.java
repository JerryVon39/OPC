package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员端登录/安全事件审计对象 reader_login_log（后台可查）
 * 事件：login / login_fail / logout / change_pwd / reset_pwd / change_email / register
 */
public class ReaderLoginLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 成员ID（登录成功时必填） */
    private Long readerId;

    /** 成员编号（失败时尽力记录） */
    private String cardNo;

    /** 来源 IP */
    private String ip;

    /** 事件：login/logout/login_fail/change_pwd/reset_pwd/change_email/register */
    private String event;

    /** 结果(0失败 1成功) */
    private String result;

    /** 备注（失败原因等） */
    private String msg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReaderId() { return readerId; }
    public void setReaderId(Long readerId) { this.readerId = readerId; }
    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
}
