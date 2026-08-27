package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CmsPage;

/** 75：自定义前台页面 Mapper */
public interface CmsPageMapper
{
    public List<CmsPage> selectCmsPageList(CmsPage cmsPage);

    public CmsPage selectCmsPageByPageId(Long pageId);

    public CmsPage selectCmsPageByPageKey(String pageKey);

    public int insertCmsPage(CmsPage cmsPage);

    public int updateCmsPage(CmsPage cmsPage);

    public int deleteCmsPageByPageId(@Param("pageId") Long pageId);

    /** 删除页面时连带删除该页全部区块 */
    public int deleteCmsBlockByPageKey(@Param("pageKey") String pageKey);
}
