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
import com.ruoyi.system.domain.SysBanner;
import com.ruoyi.system.service.ISysBannerService;

/**
 * 前台轮播图Controller
 */
@RestController
@RequestMapping("/system/banner")
public class SysBannerController extends BaseController
{
    @Autowired
    private ISysBannerService sysBannerService;

    /** 前台轮播列表（匿名，仅启用的） */
    @Anonymous
    @GetMapping("/publicList")
    public AjaxResult publicList()
    {
        SysBanner query = new SysBanner();
        query.setStatus("0");
        return success(sysBannerService.selectSysBannerList(query));
    }

    /** 后台列表 */
    @PreAuthorize("@ss.hasPermi('system:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysBanner sysBanner)
    {
        startPage();
        List<SysBanner> list = sysBannerService.selectSysBannerList(sysBanner);
        return getDataTable(list);
    }

    /** 详情 */
    @PreAuthorize("@ss.hasPermi('system:banner:query')")
    @GetMapping(value = "/{bannerId}")
    public AjaxResult getInfo(@PathVariable("bannerId") Long bannerId)
    {
        return success(sysBannerService.selectSysBannerByBannerId(bannerId));
    }

    /** 新增 */
    @PreAuthorize("@ss.hasPermi('system:banner:add')")
    @Log(title = "轮播图", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysBanner sysBanner)
    {
        return toAjax(sysBannerService.insertSysBanner(sysBanner));
    }

    /** 修改 */
    @PreAuthorize("@ss.hasPermi('system:banner:edit')")
    @Log(title = "轮播图", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysBanner sysBanner)
    {
        return toAjax(sysBannerService.updateSysBanner(sysBanner));
    }

    /** 删除 */
    @PreAuthorize("@ss.hasPermi('system:banner:remove')")
    @Log(title = "轮播图", businessType = BusinessType.DELETE)
    @DeleteMapping("/{bannerIds}")
    public AjaxResult remove(@PathVariable Long[] bannerIds)
    {
        return toAjax(sysBannerService.deleteSysBannerByBannerIds(bannerIds));
    }
}
