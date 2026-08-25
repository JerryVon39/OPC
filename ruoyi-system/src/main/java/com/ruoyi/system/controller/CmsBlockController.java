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
import com.ruoyi.system.domain.CmsBlock;
import com.ruoyi.system.domain.CmsBlockHistory;
import com.ruoyi.system.service.ICmsBlockService;

/**
 * CMS 区块Controller（前台公开区块 + 后台管理 /system/cmsBlock）
 */
@RestController
@RequestMapping("/system/cmsBlock")
public class CmsBlockController extends BaseController
{
    @Autowired
    private ICmsBlockService cmsBlockService;

    /** 前台公开区块列表（匿名，仅显示区块，供前台文本槽渐进增强加载） */
    @Anonymous
    @GetMapping("/publicList")
    public AjaxResult publicList(String pageKey)
    {
        return success(cmsBlockService.selectPublicBlockList(pageKey));
    }

    /** 后台区块列表 */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsBlock cmsBlock)
    {
        startPage();
        List<CmsBlock> list = cmsBlockService.selectCmsBlockList(cmsBlock);
        return getDataTable(list);
    }

    /** 区块详情 */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:query')")
    @GetMapping(value = "/{blockId}")
    public AjaxResult getInfo(@PathVariable("blockId") Long blockId)
    {
        return success(cmsBlockService.selectCmsBlockByBlockId(blockId));
    }

    /** 新增区块 */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:add')")
    @Log(title = "CMS区块", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsBlock cmsBlock)
    {
        return toAjax(cmsBlockService.insertCmsBlock(cmsBlock));
    }

    /** 修改区块（自动写历史 + version+1） */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:edit')")
    @Log(title = "CMS区块", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsBlock cmsBlock)
    {
        return toAjax(cmsBlockService.updateCmsBlock(cmsBlock));
    }

    /** 删除区块（含历史） */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:remove')")
    @Log(title = "CMS区块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{blockIds}")
    public AjaxResult remove(@PathVariable Long[] blockIds)
    {
        return toAjax(cmsBlockService.deleteCmsBlockByBlockIds(blockIds));
    }

    /** 区块历史列表（version 倒序，最多 20 版） */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:query')")
    @GetMapping("/history/{blockId}")
    public AjaxResult history(@PathVariable("blockId") Long blockId)
    {
        return success(cmsBlockService.selectHistoryByBlockId(blockId));
    }

    /** 回滚到指定历史版本（回滚本身记为新版本） */
    @PreAuthorize("@ss.hasPermi('system:cmsBlock:edit')")
    @Log(title = "CMS区块", businessType = BusinessType.UPDATE)
    @PutMapping("/rollback/{blockId}/{version}")
    public AjaxResult rollback(@PathVariable("blockId") Long blockId, @PathVariable("version") Long version)
    {
        return toAjax(cmsBlockService.rollbackCmsBlock(blockId, version));
    }
}
