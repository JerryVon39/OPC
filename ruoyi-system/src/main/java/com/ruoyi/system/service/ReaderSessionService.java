package com.ruoyi.system.service;

import jakarta.servlet.http.HttpServletRequest;

public interface ReaderSessionService
{
    String create(String cardNo);

    String resolve(String token);

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

    void remove(String token);

    /** 登录/补办频控：该 key（IP/证号维度）失败次数是否已达上限（拦截） */
    boolean isBlocked(String key);

    /** 记录一次失败（首次失败起 30 分钟窗口，窗口内叠加计数） */
    void recordFail(String key);

    /** 成功后清除失败计数 */
    void clearFail(String key);
}
