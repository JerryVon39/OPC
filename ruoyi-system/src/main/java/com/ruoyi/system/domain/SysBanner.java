package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 前台轮播图对象 sys_banner
 */
public class SysBanner extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 轮播ID */
    private Long bannerId;

    /** 标题 */
    private String title;

    /** 副标题 */
    private String subtitle;

    /** 图片地址(可为空,空则渐变背景) */
    private String image;

    /** 跳转链接 */
    private String link;

    /** 排序 */
    private Long sort;

    /** 状态(0启用 1停用) */
    private String status;

    public void setBannerId(Long bannerId) { this.bannerId = bannerId; }
    public Long getBannerId() { return bannerId; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getSubtitle() { return subtitle; }

    public void setImage(String image) { this.image = image; }
    public String getImage() { return image; }

    public void setLink(String link) { this.link = link; }
    public String getLink() { return link; }

    public void setSort(Long sort) { this.sort = sort; }
    public Long getSort() { return sort; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("bannerId", getBannerId())
            .append("title", getTitle())
            .append("subtitle", getSubtitle())
            .append("image", getImage())
            .append("link", getLink())
            .append("sort", getSort())
            .append("status", getStatus())
            .toString();
    }
}
