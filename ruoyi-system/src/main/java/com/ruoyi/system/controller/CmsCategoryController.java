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
import com.ruoyi.system.domain.CmsCategory;
import com.ruoyi.system.service.ICmsCategoryService;

/**
 * CMS 文章栏目Controller（后台管理 /system/cmsCategory + 前台公开栏目）
 */
@RestController
@RequestMapping("/system/cmsCategory")
public class CmsCategoryController extends BaseController
{
    @Autowired
    private ICmsCategoryService cmsCategoryService;

    /** 前台公开栏目列表（匿名，仅启用栏目，供新闻页 Tab 动态加载） */
    @Anonymous
    @GetMapping("/publicList")
    public AjaxResult publicList()
    {
        CmsCategory query = new CmsCategory();
        query.setStatus("0");
        return success(cmsCategoryService.selectCmsCategoryList(query));
    }

    /** 后台栏目列表 */
    @PreAuthorize("@ss.hasPermi('system:cmsCategory:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsCategory cmsCategory)
    {
        startPage();
        List<CmsCategory> list = cmsCategoryService.selectCmsCategoryList(cmsCategory);
        return getDataTable(list);
    }

    /** 栏目详情 */
    @PreAuthorize("@ss.hasPermi('system:cmsCategory:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(cmsCategoryService.selectCmsCategoryByCategoryId(categoryId));
    }

    /** 新增栏目 */
    @PreAuthorize("@ss.hasPermi('system:cmsCategory:add')")
    @Log(title = "CMS栏目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsCategory cmsCategory)
    {
        return toAjax(cmsCategoryService.insertCmsCategory(cmsCategory));
    }

    /** 修改栏目 */
    @PreAuthorize("@ss.hasPermi('system:cmsCategory:edit')")
    @Log(title = "CMS栏目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsCategory cmsCategory)
    {
        return toAjax(cmsCategoryService.updateCmsCategory(cmsCategory));
    }

    /** 删除栏目 */
    @PreAuthorize("@ss.hasPermi('system:cmsCategory:remove')")
    @Log(title = "CMS栏目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(cmsCategoryService.deleteCmsCategoryByCategoryIds(categoryIds));
    }
}
