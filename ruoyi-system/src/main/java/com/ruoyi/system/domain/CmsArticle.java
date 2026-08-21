package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CMS 文章对象 cms_article
 */
public class CmsArticle extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private Long articleId;

    /** 栏目ID */
    private Long categoryId;

    /** 文章标题 */
    @Excel(name = "文章标题")
    private String title;

    /** 摘要 */
    @Excel(name = "摘要")
    private String summary;

    /** 正文(BBCODE) */
    private String content;

    /** 封面图 */
    private String cover;

    /** 作者 */
    @Excel(name = "作者")
    private String author;

    /** 置顶(0普通 1置顶) */
    @Excel(name = "置顶(0普通 1置顶)")
    private String isTop;

    /** 状态(0已发布 1草稿 2已下线) */
    @Excel(name = "状态(0已发布 1草稿 2已下线)")
    private String status;

    /** 浏览量 */
    @Excel(name = "浏览量")
    private Long views;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 栏目名称（列表 LEFT JOIN 派生展示，非 cms_article 表字段） */
    private String categoryName;

    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getArticleId() { return articleId; }

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

    public void setViews(Long views) { this.views = views; }
    public Long getViews() { return views; }

    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
    public Date getPublishTime() { return publishTime; }

    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryName() { return categoryName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("articleId", getArticleId())
            .append("categoryId", getCategoryId())
            .append("title", getTitle())
            .append("summary", getSummary())
            .append("content", getContent())
            .append("cover", getCover())
            .append("author", getAuthor())
            .append("isTop", getIsTop())
            .append("status", getStatus())
            .append("views", getViews())
            .append("publishTime", getPublishTime())
            .toString();
    }
}
