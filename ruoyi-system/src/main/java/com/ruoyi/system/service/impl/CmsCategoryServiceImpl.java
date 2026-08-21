package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.CmsCategory;
import com.ruoyi.system.mapper.CmsCategoryMapper;
import com.ruoyi.system.service.ICmsCategoryService;

/**
 * CMS 文章栏目Service业务层处理
 */
@Service
public class CmsCategoryServiceImpl implements ICmsCategoryService
{
    @Autowired
    private CmsCategoryMapper cmsCategoryMapper;

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
        }
        return cmsCategoryMapper.deleteCmsCategoryByCategoryIds(categoryIds);
    }
}
