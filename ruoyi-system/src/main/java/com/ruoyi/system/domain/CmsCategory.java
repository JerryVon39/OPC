package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CMS 文章栏目对象 cms_category
 */
public class CmsCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 栏目ID */
    private Long categoryId;

    /** 栏目名称 */
    @Excel(name = "栏目名称")
    private String categoryName;

    /** 父栏目ID */
    private Long parentId;

    /** 排序 */
    @Excel(name = "排序")
    private Long sort;

    /** 状态(0启用 1停用) */
    @Excel(name = "状态(0启用 1停用)")
    private String status;

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getCategoryId() { return categoryId; }

    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryName() { return categoryName; }

    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getParentId() { return parentId; }

    public void setSort(Long sort) { this.sort = sort; }
    public Long getSort() { return sort; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("categoryId", getCategoryId())
            .append("categoryName", getCategoryName())
            .append("parentId", getParentId())
            .append("sort", getSort())
            .append("status", getStatus())
            .toString();
    }
}
