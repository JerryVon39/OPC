package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CmsPage;

/** 75：自定义前台页面 Service */
public interface ICmsPageService
{
    public List<CmsPage> selectCmsPageList(CmsPage cmsPage);

    /** 前台公开列表（仅启用，按 sort 排序） */
    public List<CmsPage> selectPublicCmsPageList();

    public int insertCmsPage(CmsPage cmsPage);

    public int updateCmsPage(CmsPage cmsPage);

    /** 删除页面 + 连带删除该页全部区块 */
    public int deleteCmsPageByPageId(Long pageId);
}
