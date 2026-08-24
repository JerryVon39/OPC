package com.ruoyi.common.utils;

import com.ruoyi.common.exception.ServiceException;

/**
 * 密码强度校验（注册/改密/重置统一规则）：
 * 至少 10 位，且包含 大写字母/小写字母/数字/符号 中的至少 3 类。
 */
public class PasswordStrength
{
    private PasswordStrength() { }

    /** 校验失败抛 ServiceException（前端展示提示语） */
    public static void check(String password)
    {
        if (password == null || password.length() < 10)
        {
            throw new ServiceException("密码长度至少 10 位");
        }
        if (password.length() > 64)
        {
            throw new ServiceException("密码长度不能超过 64 位");
        }
        int classes = 0;
        if (password.matches(".*[A-Z].*")) { classes++; }
        if (password.matches(".*[a-z].*")) { classes++; }
        if (password.matches(".*\\d.*")) { classes++; }
        if (password.matches(".*[^A-Za-z0-9].*")) { classes++; }
        if (classes < 3)
        {
            throw new ServiceException("密码需至少包含大写字母、小写字母、数字、符号中的 3 类");
        }
    }
}
