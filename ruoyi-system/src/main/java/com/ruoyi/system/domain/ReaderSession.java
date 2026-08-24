package com.ruoyi.system.domain;

/**
 * 读者端会话信息（Redis 存储，多端管理展示用）
 */
public class ReaderSession
{
    /** 成员编号 */
    private String cardNo;

    /** 设备标识（登录时前端传入，如浏览器 UA 摘要） */
    private String device;

    /** 登录 IP */
    private String ip;

    /** 最近活跃时间（毫秒时间戳） */
    private long lastActiveAt;

    public ReaderSession() { }

    public ReaderSession(String cardNo, String device, String ip, long lastActiveAt)
    {
        this.cardNo = cardNo;
        this.device = device == null ? "" : device;
        this.ip = ip == null ? "" : ip;
        this.lastActiveAt = lastActiveAt;
    }

    /** 序列化为 Redis 值：cardNo|device|ip|time（device 内不允许含 |） */
    public String toStored()
    {
        return cardNo + "|" + device.replace("|", "") + "|" + (ip == null ? "" : ip) + "|" + lastActiveAt;
    }

    /** 从 Redis 值反序列化；格式异常返回 null */
    public static ReaderSession fromStored(String stored)
    {
        if (stored == null || stored.isEmpty())
        {
            return null;
        }
        String[] parts = stored.split("\\|", -1);
        if (parts.length < 4)
        {
            return null;
        }
        try
        {
            return new ReaderSession(parts[0], parts[1], parts[2], Long.parseLong(parts[3]));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public long getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(long lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
