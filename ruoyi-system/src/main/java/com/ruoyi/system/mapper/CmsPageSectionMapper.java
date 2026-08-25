package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CmsPageSection;

/**
 * CMS 首页模块Mapper接口
 */
public interface CmsPageSectionMapper
{
    /** 后台模块列表（按 pageKey 筛选，sort 升序） */
    public List<CmsPageSection> selectCmsPageSectionList(CmsPageSection cmsPageSection);

    /** 前台公开模块列表（仅 visible='0'，sort 升序） */
    public List<CmsPageSection> selectPublicSectionList(String pageKey);

    /** 按ID查询 */
    public CmsPageSection selectCmsPageSectionBySectionId(Long sectionId);

    /** 按模块键查询（种子/去重用） */
    public CmsPageSection selectCmsPageSectionBySectionKey(String sectionKey);

    /** 相邻模块（上下移：dir='up' 取 sort 更小的最大一条；dir='down' 取 sort 更大的最小一条） */
    public CmsPageSection selectNeighborSection(@Param("pageKey") String pageKey, @Param("sort") Long sort, @Param("dir") String dir);

    /** 新增 */
    public int insertCmsPageSection(CmsPageSection cmsPageSection);

    /** 修改 */
    public int updateCmsPageSection(CmsPageSection cmsPageSection);

    /** 删除 */
    public int deleteCmsPageSectionBySectionIds(Long[] sectionIds);
}
