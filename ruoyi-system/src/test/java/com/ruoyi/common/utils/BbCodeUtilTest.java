package com.ruoyi.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BBCODE 渲染单元测试：标签转换、XSS 转义、url/img 白名单
 */
public class BbCodeUtilTest
{
    /** 空值原样返回 */
    @Test
    void render_null_returnsNull()
    {
        assertNull(BbCodeUtil.render(null));
    }

    /** 空串原样返回 */
    @Test
    void render_empty_returnsEmpty()
    {
        assertEquals("", BbCodeUtil.render(""));
    }

    /** 粗体标签 */
    @Test
    void render_bold()
    {
        assertEquals("<b>重点</b>", BbCodeUtil.render("[b]重点[/b]"));
    }

    /** 斜体 + 下划线 */
    @Test
    void render_italicAndUnderline()
    {
        assertEquals("<i><u>文本</u></i>", BbCodeUtil.render("[i][u]文本[/u][/i]"));
    }

    /** 颜色标签（白名单色值） */
    @Test
    void render_color()
    {
        assertEquals("<span style=\"color:#ff0000\">红</span>", BbCodeUtil.render("[color=#ff0000]红[/color]"));
    }

    /** 颜色标签：非法色值不渲染（保持原样转义） */
    @Test
    void render_color_invalidValue_keepsRaw()
    {
        String out = BbCodeUtil.render("[color=javascript:alert(1)]红[/color]");
        assertFalse(out.contains("<span"));
    }

    /** URL 白名单：https 正常渲染 */
    @Test
    void render_url_https()
    {
        assertEquals("<a href=\"https://example.com\" target=\"_blank\" rel=\"noopener\">官网</a>",
                BbCodeUtil.render("[url=https://example.com]官网[/url]"));
    }

    /** URL 白名单：javascript: 不渲染（防注入） */
    @Test
    void render_url_javascript_blocked()
    {
        String out = BbCodeUtil.render("[url=javascript:alert(1)]点我[/url]");
        assertFalse(out.contains("<a"));
        assertTrue(out.contains("点我"));
    }

    /** img：http 正常渲染 */
    @Test
    void render_img_http()
    {
        assertTrue(BbCodeUtil.render("[img]http://a.com/pic.jpg[/img]").contains("<img src=\"http://a.com/pic.jpg\""));
    }

    /** XSS：script 标签被整体转义 */
    @Test
    void render_xss_scriptEscaped()
    {
        String out = BbCodeUtil.render("<script>alert(1)</script>");
        assertFalse(out.contains("<script>"));
        assertTrue(out.contains("&lt;script&gt;"));
    }

    /** XSS：[b] 内的 HTML 仍被转义 */
    @Test
    void render_xss_insideTagEscaped()
    {
        String out = BbCodeUtil.render("[b]<img src=x onerror=alert(1)>[/b]");
        assertTrue(out.contains("&lt;img"));
        assertFalse(out.contains("<img"));
    }

    /** 换行转 <br/> */
    @Test
    void render_newline()
    {
        assertEquals("第一行<br/>第二行", BbCodeUtil.render("第一行\n第二行"));
    }

    /** 未闭合标签：原样保留（不抛异常） */
    @Test
    void render_unclosedTag_keepsRaw()
    {
        assertEquals("[b]未闭合", BbCodeUtil.render("[b]未闭合"));
    }
}
