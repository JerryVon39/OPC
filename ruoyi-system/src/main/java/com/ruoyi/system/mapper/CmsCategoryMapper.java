package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CmsCategory;

/**
 * CMS 文章栏目Mapper接口
 */
public interface CmsCategoryMapper
{
    /** 按ID查询栏目 */
    public CmsCategory selectCmsCategoryByCategoryId(Long categoryId);

    /** 栏目列表（支持栏目名/状态筛选，按 sort 排序） */
    public List<CmsCategory> selectCmsCategoryList(CmsCategory cmsCategory);

    /** 新增栏目 */
    public int insertCmsCategory(CmsCategory cmsCategory);

    /** 修改栏目 */
    public int updateCmsCategory(CmsCategory cmsCategory);

    /** 批量删除栏目 */
    public int deleteCmsCategoryByCategoryIds(Long[] categoryIds);

    /** B：栏目文章数聚合（未删除文章按栏目分组） */
    public java.util.List<java.util.Map<String, Object>> selectArticleCounts();
}
