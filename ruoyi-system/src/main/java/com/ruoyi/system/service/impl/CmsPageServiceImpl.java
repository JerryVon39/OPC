package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.CmsPage;
import com.ruoyi.system.mapper.CmsPageMapper;
import com.ruoyi.system.service.ICmsPageService;

/** 75：自定义前台页面 Service 实现 */
@Service
public class CmsPageServiceImpl implements ICmsPageService
{
    /** 内置页面键（不可被自定义页占用，防冲突） */
    private static final String[] BUILTIN_KEYS = { "home", "about", "join", "talent", "industry", "news", "policy", "article", "profile", "page", "block-preview" };

    @Autowired
    private CmsPageMapper cmsPageMapper;

    @Override
    public List<CmsPage> selectCmsPageList(CmsPage cmsPage)
    {
        return cmsPageMapper.selectCmsPageList(cmsPage);
    }

    @Override
    public List<CmsPage> selectPublicCmsPageList()
    {
        CmsPage query = new CmsPage();
        query.setStatus("0");
        return cmsPageMapper.selectCmsPageList(query);
    }

    @Override
    public int insertCmsPage(CmsPage cmsPage)
    {
        if (cmsPage == null || cmsPage.getPageKey() == null || cmsPage.getPageKey().trim().isEmpty())
        {
            throw new ServiceException("页面标识不能为空");
        }
        String key = cmsPage.getPageKey().trim().toLowerCase();
        // 格式校验：小写字母数字连字符
        if (!key.matches("[a-z0-9-]{1,50}"))
        {
            throw new ServiceException("页面标识仅支持小写字母、数字、连字符（如 activity-2026）");
        }
        // 内置页冲突校验
        for (String bk : BUILTIN_KEYS)
        {
            if (bk.equals(key))
            {
                throw new ServiceException("页面标识与内置页面冲突：" + bk);
            }
        }
        // 唯一校验
        if (cmsPageMapper.selectCmsPageByPageKey(key) != null)
        {
            throw new ServiceException("页面标识已存在：" + key);
        }
        if (cmsPage.getPageName() == null || cmsPage.getPageName().trim().isEmpty())
        {
            throw new ServiceException("页面名称不能为空");
        }
        cmsPage.setPageKey(key);
        cmsPage.setPageName(cmsPage.getPageName().trim());
        cmsPage.setSort(cmsPage.getSort() == null ? 0L : cmsPage.getSort());
        cmsPage.setStatus(cmsPage.getStatus() == null ? "0" : cmsPage.getStatus());
        return cmsPageMapper.insertCmsPage(cmsPage);
    }

    @Override
    public int updateCmsPage(CmsPage cmsPage)
    {
        if (cmsPage.getPageName() != null && cmsPage.getPageName().trim().isEmpty())
        {
            throw new ServiceException("页面名称不能为空");
        }
        return cmsPageMapper.updateCmsPage(cmsPage);
    }

    @Override
    public int deleteCmsPageByPageId(Long pageId)
    {
        CmsPage page = cmsPageMapper.selectCmsPageByPageId(pageId);
        if (page == null)
        {
            throw new ServiceException("页面不存在");
        }
        // 连带删除该页全部区块（前台该页即空）
        cmsPageMapper.deleteCmsBlockByPageKey(page.getPageKey());
        return cmsPageMapper.deleteCmsPageByPageId(pageId);
    }
}
