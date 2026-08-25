package com.ruoyi.system.service;

import java.util.List;

import com.ruoyi.system.domain.ReaderSession;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 成员端会话服务：Redis 存储，14 天滑动续期（时长 sys_config reader.session.minutes 可配），
 * 支持登出/多端列表/退出其他设备；登录失败频控保留。
 */
public interface ReaderSessionService
{
    /** 创建会话（无设备信息场景，如后台补登） */
    String create(String cardNo);

    /** 创建会话（前台登录/注册，记录设备与 IP 供多端管理） */
    String create(String cardNo, String ip, String device);

    /** 解析令牌（每次解析刷新 TTL 实现滑动续期），无效返回 null */
    String resolve(String token);

    /** 解析完整会话信息（滑动续期），无效返回 null */
    ReaderSession resolveInfo(String token);

    /** 从请求解析会话令牌：优先取 X-Session-Token 请求头（凭证不落 URL/日志），
     *  兼容旧参数传递（sessionToken 参数兜底，过渡期后移除） */
    default String resolveFromRequest(HttpServletRequest request)
    {
        if (request == null) return null;
        String token = request.getHeader("X-Session-Token");
        if (token == null || token.trim().isEmpty())
        {
            token = request.getParameter("sessionToken");
        }
        return resolve(token);
    }

    /** 某成员的全部会话（多端管理列表，按最近活跃倒序） */
    List<ReaderSession> listSessions(String cardNo);

    /** 退出其他所有设备（保留当前 token），返回退出数量 */
    int revokeOthers(String cardNo, String currentToken);

    /** 删除会话（登出） */
    void remove(String token);

    /** 登录/补办频控：该 key（IP/证号维度）失败次数已达上限（5 次/30 分钟）或处于递增退避期 → true */
    boolean isBlocked(String key);

    /** 记录一次失败（30 分钟窗口叠加计数 + 递增退避 1/2/5/15 分钟档位） */
    void recordFail(String key);

    /** 成功后清除失败计数与退避 */
    void clearFail(String key);

    /** IP 全局限速：该 IP 60 秒窗口内请求次数是否已达上限（10 次/分钟） */
    boolean isIpRateLimited(String ip);

    /** 记录一次该 IP 的请求（60 秒窗口计数） */
    void recordIpRequest(String ip);
}
