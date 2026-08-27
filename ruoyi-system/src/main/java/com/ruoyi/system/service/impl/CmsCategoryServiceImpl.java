package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.CmsCategory;
import com.ruoyi.system.mapper.CmsArticleMapper;
import com.ruoyi.system.mapper.CmsCategoryMapper;
import com.ruoyi.system.service.ICmsCategoryService;

/**
 * CMS 文章栏目Service业务层处理
 */
@Service
@Transactional
public class CmsCategoryServiceImpl implements ICmsCategoryService
{
    @Autowired
    private CmsCategoryMapper cmsCategoryMapper;

    @Autowired
    private CmsArticleMapper cmsArticleMapper;

    @Override
    public CmsCategory selectCmsCategoryByCategoryId(Long categoryId)
    {
        return cmsCategoryMapper.selectCmsCategoryByCategoryId(categoryId);
    }

    @Override
    public List<CmsCategory> selectCmsCategoryList(CmsCategory cmsCategory)
    {
        return cmsCategoryMapper.selectCmsCategoryList(cmsCategory);
    }

    @Override
    public int insertCmsCategory(CmsCategory cmsCategory)
    {
        if (cmsCategory == null || cmsCategory.getCategoryName() == null || cmsCategory.getCategoryName().trim().isEmpty())
        {
            throw new ServiceException("栏目名称不能为空");
        }
        // 栏目名唯一性校验（幂等语义与 SQL 种子一致：同名栏目不允许重复创建）
        CmsCategory query = new CmsCategory();
        query.setCategoryName(cmsCategory.getCategoryName().trim());
        List<CmsCategory> exists = cmsCategoryMapper.selectCmsCategoryList(query);
        if (exists != null && !exists.isEmpty())
        {
            throw new ServiceException("栏目「" + cmsCategory.getCategoryName().trim() + "」已存在");
        }
        return cmsCategoryMapper.insertCmsCategory(cmsCategory);
    }

    @Override
    public int updateCmsCategory(CmsCategory cmsCategory)
    {
        if (cmsCategory == null || cmsCategory.getCategoryId() == null)
        {
            throw new ServiceException("栏目ID不能为空");
        }
        if (cmsCategoryMapper.selectCmsCategoryByCategoryId(cmsCategory.getCategoryId()) == null)
        {
            throw new ServiceException("栏目不存在或已删除");
        }
        return cmsCategoryMapper.updateCmsCategory(cmsCategory);
    }

    @Override
    public int deleteCmsCategoryByCategoryIds(Long[] categoryIds)
    {
        if (categoryIds == null || categoryIds.length == 0)
        {
            return 0;
        }
        for (Long categoryId : categoryIds)
        {
            if (categoryId == null || cmsCategoryMapper.selectCmsCategoryByCategoryId(categoryId) == null)
            {
                throw new ServiceException("部分栏目不存在或已删除");
            }
            // M3 守卫：栏目下还有文章时禁止删除（否则文章变孤儿，前台所有 Tab 消失且后台无法找回）
            if (cmsArticleMapper.countCmsArticleByCategoryId(categoryId) > 0)
            {
                throw new ServiceException("该栏目下仍有文章，请先移走或删除栏目内文章后再删除栏目");
            }
            // M3.1 守卫：栏目下还有子栏目时禁止删除（否则子栏目变孤儿，分类树悬空）
            CmsCategory childQuery = new CmsCategory();
            childQuery.setParentId(categoryId);
            List<CmsCategory> children = cmsCategoryMapper.selectCmsCategoryList(childQuery);
            if (children != null && !children.isEmpty())
            {
                throw new ServiceException("该栏目下存在子栏目，请先删除或移动子栏目后再删除栏目");
            }
        }
        return cmsCategoryMapper.deleteCmsCategoryByCategoryIds(categoryIds);
    }

    /** B：栏目文章数聚合 */
    @Override
    public java.util.List<java.util.Map<String, Object>> selectArticleCounts()
    {
        return cmsCategoryMapper.selectArticleCounts();
    }
}
