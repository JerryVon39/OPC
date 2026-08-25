package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.CmsPageSection;
import com.ruoyi.system.service.ICmsPageSectionService;

/**
 * CMS 首页模块Controller（前台公开模块列表 + 后台搭建管理 /system/cmsSection）
 */
@RestController
@RequestMapping("/system/cmsSection")
public class CmsPageSectionController extends BaseController
{
    @Autowired
    private ICmsPageSectionService cmsPageSectionService;

    /** 前台公开模块列表（匿名，按 sort 升序，供首页模块化渲染） */
    @Anonymous
    @GetMapping("/publicList")
    public AjaxResult publicList(String pageKey)
    {
        return success(cmsPageSectionService.selectPublicSectionList(pageKey));
    }

    /** 后台模块列表 */
    @PreAuthorize("@ss.hasPermi('system:cmsSection:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsPageSection cmsPageSection)
    {
        startPage();
        List<CmsPageSection> list = cmsPageSectionService.selectCmsPageSectionList(cmsPageSection);
        return getDataTable(list);
    }

    /** 模块详情 */
    @PreAuthorize("@ss.hasPermi('system:cmsSection:query')")
    @GetMapping(value = "/{sectionId}")
    public AjaxResult getInfo(@PathVariable("sectionId") Long sectionId)
    {
        return success(cmsPageSectionService.selectCmsPageSectionBySectionId(sectionId));
    }

    /** 新增模块 */
    @PreAuthorize("@ss.hasPermi('system:cmsSection:add')")
    @Log(title = "首页模块", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsPageSection cmsPageSection)
    {
        return toAjax(cmsPageSectionService.insertCmsPageSection(cmsPageSection));
    }

    /** 修改模块 */
    @PreAuthorize("@ss.hasPermi('system:cmsSection:edit')")
    @Log(title = "首页模块", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsPageSection cmsPageSection)
    {
        return toAjax(cmsPageSectionService.updateCmsPageSection(cmsPageSection));
    }

    /** 删除模块 */
    @PreAuthorize("@ss.hasPermi('system:cmsSection:remove')")
    @Log(title = "首页模块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{sectionIds}")
    public AjaxResult remove(@PathVariable Long[] sectionIds)
    {
        return toAjax(cmsPageSectionService.deleteCmsPageSectionBySectionIds(sectionIds));
    }

    /** 模块上移/下移（交换相邻 sort） */
    @PreAuthorize("@ss.hasPermi('system:cmsSection:sort')")
    @Log(title = "首页模块", businessType = BusinessType.UPDATE)
    @PutMapping("/move/{sectionId}/{dir}")
    public AjaxResult move(@PathVariable("sectionId") Long sectionId, @PathVariable("dir") String dir)
    {
        return toAjax(cmsPageSectionService.moveSection(sectionId, dir));
    }
}
