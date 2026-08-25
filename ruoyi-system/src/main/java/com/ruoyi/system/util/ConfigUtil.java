package com.ruoyi.system.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 系统参数读取工具（带默认值 + 按成员类型取键）
 */
@Component
public class ConfigUtil
{
    @Autowired
    private ISysConfigService configService;

    /** 读取整数参数，异常/空回退默认值 */
    public int getInt(String key, int def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            if (v != null && !v.isEmpty())
            {
                return Integer.parseInt(v);
            }
        }
        catch (Exception ignore) { }
        return def;
    }

    /** 读取小数参数（如罚款单价 0.10），异常/空回退默认值 */
    public double getDouble(String key, double def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            if (v != null && !v.isEmpty())
            {
                return Double.parseDouble(v);
            }
        }
        catch (Exception ignore) { }
        return def;
    }

    /**
     * 按成员类型取参数整数：book.borrow.maxCount.student（字典值1学生/2教师/3普通→语义后缀），
     * 无类型参数时回退通用键
     */
    public int getTypeInt(String prefix, String readerType, int def)
    {
        String suffix = "1".equals(readerType) ? "student"
                : "2".equals(readerType) ? "teacher"
                : "3".equals(readerType) ? "normal" : "";
        if (!suffix.isEmpty())
        {
            int v = getInt(prefix + "." + suffix, Integer.MIN_VALUE);
            if (v != Integer.MIN_VALUE)
            {
                return v;
            }
        }
        return getInt(prefix, def);
    }
}
