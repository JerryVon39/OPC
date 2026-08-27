package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.CmsPage;
import com.ruoyi.system.service.ICmsPageService;

/** 75：自定义前台页面 Controller（后台管理 + 前台公开列表） */
@RestController
@RequestMapping("/system/cmsPage")
public class CmsPageController extends BaseController
{
    @Autowired
    private ICmsPageService cmsPageService;

    /** 后台列表（区块管理 Tab 动态化用） */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsPage cmsPage)
    {
        return success(cmsPageService.selectCmsPageList(cmsPage));
    }

    /** 前台公开列表（仅启用；前台「更多」菜单自动追加用） */
    @Anonymous
    @GetMapping("/publicList")
    public AjaxResult publicList()
    {
        return success(cmsPageService.selectPublicCmsPageList());
    }

    @PreAuthorize("@ss.hasPermi('system:cmsBlock:add')")
    @PostMapping
    public AjaxResult add(@RequestBody CmsPage cmsPage)
    {
        cmsPage.setCreateBy(getUsername());
        return toAjax(cmsPageService.insertCmsPage(cmsPage));
    }

    @PreAuthorize("@ss.hasPermi('system:cmsBlock:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody CmsPage cmsPage)
    {
        cmsPage.setUpdateBy(getUsername());
        return toAjax(cmsPageService.updateCmsPage(cmsPage));
    }

    /** 删除页面（连带删除该页全部区块，确认弹窗在前端） */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:remove')")
    @DeleteMapping("/{pageId}")
    public AjaxResult remove(@PathVariable("pageId") Long pageId)
    {
        return toAjax(cmsPageService.deleteCmsPageByPageId(pageId));
    }
}
