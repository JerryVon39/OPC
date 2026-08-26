package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CMS 文章历史版本对象 cms_article_history（每篇最多 20 版，保存前写当前版）
 */
public class CmsArticleHistory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long historyId;
    private Long articleId;
    private Long version;
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private String author;
    private String isTop;
    private String status;
    private Long sort;
    private String attachment;
    private String keywords;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public void setHistoryId(Long historyId) { this.historyId = historyId; }
    public Long getHistoryId() { return historyId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getArticleId() { return articleId; }
    public void setVersion(Long version) { this.version = version; }
    public Long getVersion() { return version; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getCategoryId() { return categoryId; }
    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSummary() { return summary; }
    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }
    public void setCover(String cover) { this.cover = cover; }
    public String getCover() { return cover; }
    public void setAuthor(String author) { this.author = author; }
    public String getAuthor() { return author; }
    public void setIsTop(String isTop) { this.isTop = isTop; }
    public String getIsTop() { return isTop; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public void setSort(Long sort) { this.sort = sort; }
    public Long getSort() { return sort; }
    public void setAttachment(String attachment) { this.attachment = attachment; }
    public String getAttachment() { return attachment; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getKeywords() { return keywords; }
    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
    public Date getPublishTime() { return publishTime; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Date getUpdateTime() { return updateTime; }
}
