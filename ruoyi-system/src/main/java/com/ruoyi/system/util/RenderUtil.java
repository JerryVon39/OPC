package com.ruoyi.system.util;

import java.util.List;
import com.ruoyi.common.utils.BbCodeUtil;
import com.ruoyi.system.domain.Book;
import com.ruoyi.system.domain.SysNotice;

/**
 * 展示字段渲染工具：对列表统一执行 BBCODE 渲染
 */
public class RenderUtil
{
    private RenderUtil() { }

    /** 服务简介渲染 BBCODE（展示为富文本；后台编辑回显走 getInfo 不受影响） */
    public static void renderBookIntro(List<Book> books)
    {
        if (books == null)
        {
            return;
        }
        for (Book b : books)
        {
            b.setIntro(BbCodeUtil.render(b.getIntro()));
        }
    }

    /** 公告内容渲染 BBCODE */
    public static void renderNoticeContent(List<SysNotice> notices)
    {
        if (notices == null)
        {
            return;
        }
        for (SysNotice n : notices)
        {
            n.setNoticeContent(BbCodeUtil.render(n.getNoticeContent()));
        }
    }
}
