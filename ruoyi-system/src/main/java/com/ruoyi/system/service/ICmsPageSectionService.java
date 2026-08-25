package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CmsPageSection;

/**
 * CMS 首页模块Service接口
 */
public interface ICmsPageSectionService
{
    /** 后台模块列表 */
    public List<CmsPageSection> selectCmsPageSectionList(CmsPageSection cmsPageSection);

    /** 前台公开模块列表（仅 visible='0'，sort 升序） */
    public List<CmsPageSection> selectPublicSectionList(String pageKey);

    /** 按ID查询 */
    public CmsPageSection selectCmsPageSectionBySectionId(Long sectionId);

    /** 新增模块 */
    public int insertCmsPageSection(CmsPageSection cmsPageSection);

    /** 修改模块 */
    public int updateCmsPageSection(CmsPageSection cmsPageSection);

    /** 删除模块 */
    public int deleteCmsPageSectionBySectionIds(Long[] sectionIds);

    /** 上移/下移（交换相邻 sort；返回 0 表示已到边界） */
    public int moveSection(Long sectionId, String dir);
}
