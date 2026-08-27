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

    /** 多栏目ID（逗号分隔，前台列表"全部"筛选用，非表字段） */
    private String categoryIds;

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

    /** 定时下线时间（NULL=长期有效，到点前台自动隐藏） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date offlineTime;

    /** 排序(越小越靠前，置顶之后生效) */
    private Long sort;

    /** 当前版本号（历史回滚用） */
    private Long version;

    /** 附件(政策原文PDF等) */
    private String attachment;

    /** SEO关键词 */
    private String keywords;

    /** SEO描述 */
    private String description;

    /** 删除标志(0存在 2已删除，对齐 book 两态软删) */
    private String delFlag;

    /** 删除时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deletedTime;

    /** 删除人 */
    private String deletedBy;

    /** 栏目名称（列表 LEFT JOIN 派生展示，非 cms_article 表字段） */
    private String categoryName;

    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getArticleId() { return articleId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getCategoryId() { return categoryId; }

    public String getCategoryIds() { return categoryIds; }
    public void setCategoryIds(String categoryIds) { this.categoryIds = categoryIds; }

    /** 6：批量移动栏目目标文章集合（批量接口入参） */
    private Long[] articleIds;
    public Long[] getArticleIds() { return articleIds; }
    public void setArticleIds(Long[] articleIds) { this.articleIds = articleIds; }

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
    public void setOfflineTime(Date offlineTime) { this.offlineTime = offlineTime; }
    public Date getOfflineTime() { return offlineTime; }

    public void setSort(Long sort) { this.sort = sort; }
    public Long getSort() { return sort; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public void setAttachment(String attachment) { this.attachment = attachment; }
    public String getAttachment() { return attachment; }

    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getKeywords() { return keywords; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getDelFlag() { return delFlag; }

    public void setDeletedTime(Date deletedTime) { this.deletedTime = deletedTime; }
    public Date getDeletedTime() { return deletedTime; }

    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    public String getDeletedBy() { return deletedBy; }

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
