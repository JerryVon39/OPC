package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 75：自定义前台页面注册表 */
public class CmsPage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long pageId;
    /** 页面标识（前台 page.html?key=xxx；小写字母数字连字符） */
    private String pageKey;
    /** 页面名称（后台 Tab 与前台更多菜单显示） */
    private String pageName;
    /** 排序（越小越靠前） */
    private Long sort;
    /** 状态（0启用 1停用） */
    private String status;
    /** 前台入口位置（more=更多菜单 nav=顶部主导航） */
    private String menuPos;
    /** 页头大标题（空=不显示页头） */
    private String heroTitle;
    /** 页头副标题 */
    private String heroSubtitle;
    /** 页头背景（图片URL或CSS色值，空=默认深蓝渐变） */
    private String heroBg;

    public Long getPageId() { return pageId; }
    public void setPageId(Long pageId) { this.pageId = pageId; }
    public String getPageKey() { return pageKey; }
    public void setPageKey(String pageKey) { this.pageKey = pageKey; }
    public String getPageName() { return pageName; }
    public void setPageName(String pageName) { this.pageName = pageName; }
    public Long getSort() { return sort; }
    public void setSort(Long sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMenuPos() { return menuPos; }
    public void setMenuPos(String menuPos) { this.menuPos = menuPos; }
    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public String getHeroBg() { return heroBg; }
    public void setHeroBg(String heroBg) { this.heroBg = heroBg; }
}
