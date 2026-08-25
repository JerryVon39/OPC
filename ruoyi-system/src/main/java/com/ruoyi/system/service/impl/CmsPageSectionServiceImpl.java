package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.CmsPageSection;
import com.ruoyi.system.mapper.CmsPageSectionMapper;
import com.ruoyi.system.service.ICmsPageSectionService;

/**
 * CMS 首页模块Service业务层处理（含上移/下移排序）
 */
@Service
public class CmsPageSectionServiceImpl implements ICmsPageSectionService
{
    @Autowired
    private CmsPageSectionMapper cmsPageSectionMapper;

    @Override
    public List<CmsPageSection> selectCmsPageSectionList(CmsPageSection cmsPageSection)
    {
        return cmsPageSectionMapper.selectCmsPageSectionList(cmsPageSection);
    }

    @Override
    public List<CmsPageSection> selectPublicSectionList(String pageKey)
    {
        return cmsPageSectionMapper.selectPublicSectionList(pageKey);
    }

    @Override
    public CmsPageSection selectCmsPageSectionBySectionId(Long sectionId)
    {
        return cmsPageSectionMapper.selectCmsPageSectionBySectionId(sectionId);
    }

    @Override
    public int insertCmsPageSection(CmsPageSection cmsPageSection)
    {
        if (cmsPageSection == null || cmsPageSection.getSectionKey() == null || cmsPageSection.getSectionKey().trim().isEmpty())
        {
            throw new ServiceException("模块键不能为空");
        }
        if (cmsPageSectionMapper.selectCmsPageSectionBySectionKey(cmsPageSection.getSectionKey()) != null)
        {
            throw new ServiceException("模块键已存在：" + cmsPageSection.getSectionKey());
        }
        if (cmsPageSection.getTemplate() == null || cmsPageSection.getTemplate().trim().isEmpty())
        {
            throw new ServiceException("模板类型不能为空");
        }
        if (cmsPageSection.getSort() == null)
        {
            cmsPageSection.setSort(1L);
        }
        cmsPageSection.setUpdateBy(operator());
        cmsPageSection.setUpdateTime(new Date());
        return cmsPageSectionMapper.insertCmsPageSection(cmsPageSection);
    }

    @Override
    public int updateCmsPageSection(CmsPageSection cmsPageSection)
    {
        if (cmsPageSection == null || cmsPageSection.getSectionId() == null)
        {
            throw new ServiceException("模块ID不能为空");
        }
        if (cmsPageSectionMapper.selectCmsPageSectionBySectionId(cmsPageSection.getSectionId()) == null)
        {
            throw new ServiceException("模块不存在");
        }
        cmsPageSection.setUpdateBy(operator());
        cmsPageSection.setUpdateTime(new Date());
        return cmsPageSectionMapper.updateCmsPageSection(cmsPageSection);
    }

    @Override
    public int deleteCmsPageSectionBySectionIds(Long[] sectionIds)
    {
        if (sectionIds == null || sectionIds.length == 0)
        {
            return 0;
        }
        return cmsPageSectionMapper.deleteCmsPageSectionBySectionIds(sectionIds);
    }

    @Override
    public int moveSection(Long sectionId, String dir)
    {
        if (sectionId == null || (!"up".equals(dir) && !"down".equals(dir)))
        {
            throw new ServiceException("移动参数不合法");
        }
        CmsPageSection current = cmsPageSectionMapper.selectCmsPageSectionBySectionId(sectionId);
        if (current == null)
        {
            throw new ServiceException("模块不存在");
        }
        // 找相邻模块（sort 邻近），交换 sort 值（避免整体重排）
        CmsPageSection neighbor = cmsPageSectionMapper.selectNeighborSection(
            current.getPageKey(), current.getSort() == null ? 0L : current.getSort(), dir);
        if (neighbor == null)
        {
            return 0; // 已到边界，无可移动
        }
        Long curSort = current.getSort();
        CmsPageSection a = new CmsPageSection();
        a.setSectionId(current.getSectionId());
        a.setSort(neighbor.getSort());
        CmsPageSection b = new CmsPageSection();
        b.setSectionId(neighbor.getSectionId());
        b.setSort(curSort);
        cmsPageSectionMapper.updateCmsPageSection(a);
        cmsPageSectionMapper.updateCmsPageSection(b);
        return 1;
    }

    /** 当前登录用户名（未登录容错为空串） */
    private String operator()
    {
        try { return SecurityUtils.getUsername(); }
        catch (Exception e) { return ""; }
    }
}
