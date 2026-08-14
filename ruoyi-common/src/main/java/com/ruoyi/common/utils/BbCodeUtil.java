package com.ruoyi.common.utils;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * BBCODE 轻量标记渲染工具
 *
 * 支持标签：[b][i][u][color][size][url][img][quote][code][center]
 * 安全策略：先整体 HTML 转义（脚本天然失效），再替换标签；
 * url/img 仅允许 http/https/站内相对路径（防 javascript: 注入）
 */
public class BbCodeUtil
{
    private BbCodeUtil() { }

    public static String render(String text)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }
        // 1. HTML 转义（防 XSS）
        String s = StringEscapeUtils.escapeHtml4(text);
        // 2. BBCODE → HTML（非贪婪单层匹配，简单场景足够）
        s = s.replaceAll("\\[b\\](.*?)\\[/b\\]", "<b>$1</b>");
        s = s.replaceAll("\\[i\\](.*?)\\[/i\\]", "<i>$1</i>");
        s = s.replaceAll("\\[u\\](.*?)\\[/u\\]", "<u>$1</u>");
        s = s.replaceAll("\\[color=([a-zA-Z0-9#]{3,7})\\](.*?)\\[/color\\]", "<span style=\"color:$1\">$2</span>");
        s = s.replaceAll("\\[size=([0-9]{1,2})\\](.*?)\\[/size\\]", "<span style=\"font-size:${1}px\">$2</span>");
        s = s.replaceAll("\\[url=((?:https?|#)[^\\]\\s]+)\\](.*?)\\[/url\\]", "<a href=\"$1\" target=\"_blank\" rel=\"noopener\">$2</a>");
        s = s.replaceAll("\\[img\\]((?:https?|/)[^\\]\\s]+)\\[/img\\]", "<img src=\"$1\" style=\"max-width:100%;border-radius:6px\" />");
        s = s.replaceAll("\\[quote\\](.*?)\\[/quote\\]", "<blockquote style=\"margin:6px 0;padding:8px 12px;border-left:3px solid #d4a24e;background:#faf8f3;color:#666\">$1</blockquote>");
        s = s.replaceAll("\\[code\\](.*?)\\[/code\\]", "<pre style=\"background:#2d3a4b;color:#e8e8e8;padding:10px;border-radius:6px;overflow-x:auto\">$1</pre>");
        s = s.replaceAll("\\[center\\](.*?)\\[/center\\]", "<div style=\"text-align:center\">$1</div>");
        // 3. 换行 → <br/>
        s = s.replace("\n", "<br/>");
        return s;
    }
}
