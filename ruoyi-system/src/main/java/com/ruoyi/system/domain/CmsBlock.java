package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CMS 区块对象 cms_block（前台可编辑文本槽/首页区块）
 */
public class CmsBlock extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 区块ID */
    private Long blockId;

    /** 区块键(唯一，前台槽位映射) */
    private String blockKey;

    /** 页面键(home/about/join/talent/industry) */
    private String pageKey;

    /** 标题(文本槽时对应元素) */
    private String title;

    /** 副标题 */
    private String subtitle;

    /** 内容(文本槽=纯文本；html 槽=白名单过滤后的 HTML) */
    private String content;

    /** 图片(预留) */
    private String image;

    /** 链接(预留) */
    private String link;

    /** 排序 */
    private Long sort;

    /** 显示(0显示 1隐藏) */
    private String visible;

    /** 当前版本 */
    private Long version;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public void setBlockId(Long blockId) { this.blockId = blockId; }
    public Long getBlockId() { return blockId; }

    public void setBlockKey(String blockKey) { this.blockKey = blockKey; }
    public String getBlockKey() { return blockKey; }

    public void setPageKey(String pageKey) { this.pageKey = pageKey; }
    public String getPageKey() { return pageKey; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getSubtitle() { return subtitle; }

    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }

    public void setImage(String image) { this.image = image; }
    public String getImage() { return image; }

    public void setLink(String link) { this.link = link; }
    public String getLink() { return link; }

    public void setSort(Long sort) { this.sort = sort; }
    public Long getSort() { return sort; }

    public void setVisible(String visible) { this.visible = visible; }
    public String getVisible() { return visible; }

    public void setVersion(Long version) { this.version = version; }
    public Long getVersion() { return version; }

    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public String getUpdateBy() { return updateBy; }

    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Date getUpdateTime() { return updateTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("blockId", getBlockId())
            .append("blockKey", getBlockKey())
            .append("pageKey", getPageKey())
            .append("title", getTitle())
            .append("version", getVersion())
            .append("visible", getVisible())
            .toString();
    }
}
