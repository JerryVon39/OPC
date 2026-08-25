package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CMS 首页模块对象 cms_page_section（模板化搭建）
 */
public class CmsPageSection extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模块ID */
    private Long sectionId;

    /** 页面键(当前仅 home) */
    private String pageKey;

    /** 模块键(唯一) */
    private String sectionKey;

    /** 模板类型(hero/cards/tags/news/timeline/contact/cta/text/banner_text) */
    private String template;

    /** 模块标题(展示用) */
    private String title;

    /** 模板配置 JSON */
    private String configJson;

    /** 排序 */
    private Long sort;

    /** 显示(0显示 1隐藏) */
    private String visible;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
    public Long getSectionId() { return sectionId; }

    public void setPageKey(String pageKey) { this.pageKey = pageKey; }
    public String getPageKey() { return pageKey; }

    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }
    public String getSectionKey() { return sectionKey; }

    public void setTemplate(String template) { this.template = template; }
    public String getTemplate() { return template; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getConfigJson() { return configJson; }

    public void setSort(Long sort) { this.sort = sort; }
    public Long getSort() { return sort; }

    public void setVisible(String visible) { this.visible = visible; }
    public String getVisible() { return visible; }

    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public String getUpdateBy() { return updateBy; }

    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Date getUpdateTime() { return updateTime; }
}
