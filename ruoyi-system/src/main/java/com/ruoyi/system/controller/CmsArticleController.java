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
import com.ruoyi.system.domain.CmsArticle;
import com.ruoyi.system.service.ICmsArticleService;

/**
 * CMS 文章Controller（后台管理 /system/cms + 前台公开接口）
 */
@RestController
@RequestMapping("/system/cms")
public class CmsArticleController extends BaseController
{
    @Autowired
    private ICmsArticleService cmsArticleService;

    /** 前台公开文章列表（匿名）：仅已发布，置顶优先、发布时间倒序，支持栏目筛选与分页（pageNum/pageSize） */
    @Anonymous
    @GetMapping("/publicList")
    public TableDataInfo publicList(CmsArticle cmsArticle)
    {
        startPage();
        List<CmsArticle> list = cmsArticleService.selectPublicArticleList(cmsArticle);
        return getDataTable(list);
    }

    /** 前台公开文章详情（匿名）：浏览量 +1 */
    @Anonymous
    @GetMapping("/publicDetail/{articleId}")
    public AjaxResult publicDetail(@PathVariable("articleId") Long articleId)
    {
        return success(cmsArticleService.selectPublicArticleDetail(articleId));
    }

    /** 后台文章列表 */
    @PreAuthorize("@ss.hasPermi('system:cms:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsArticle cmsArticle)
    {
        startPage();
        List<CmsArticle> list = cmsArticleService.selectCmsArticleList(cmsArticle);
        return getDataTable(list);
    }

    /** 文章详情（后台编辑回显） */
    @PreAuthorize("@ss.hasPermi('system:cms:query')")
    @GetMapping(value = "/{articleId}")
    public AjaxResult getInfo(@PathVariable("articleId") Long articleId)
    {
        return success(cmsArticleService.selectCmsArticleByArticleId(articleId));
    }

    /** 新增文章 */
    @PreAuthorize("@ss.hasPermi('system:cms:add')")
    @Log(title = "CMS文章", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsArticle cmsArticle)
    {
        return toAjax(cmsArticleService.insertCmsArticle(cmsArticle));
    }

    /** 修改文章 */
    @PreAuthorize("@ss.hasPermi('system:cms:edit')")
    @Log(title = "CMS文章", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsArticle cmsArticle)
    {
        return toAjax(cmsArticleService.updateCmsArticle(cmsArticle));
    }

    /** 状态切换（后台列表开关）：0已发布 1草稿 2已下线 */
    @PreAuthorize("@ss.hasPermi('system:cms:edit')")
    @Log(title = "CMS文章", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(Long articleId, String status)
    {
        return toAjax(cmsArticleService.changeArticleStatus(articleId, status));
    }

    /** 发布：草稿/已下线 → 已发布（首次发布写入发布时间） */
    @PreAuthorize("@ss.hasPermi('system:cms:publish')")
    @Log(title = "CMS文章", businessType = BusinessType.UPDATE)
    @PutMapping("/publish")
    public AjaxResult publish(Long articleId)
    {
        return toAjax(cmsArticleService.publishArticle(articleId));
    }

    /** 删除文章 */
    @PreAuthorize("@ss.hasPermi('system:cms:remove')")
    @Log(title = "CMS文章", businessType = BusinessType.DELETE)
    @DeleteMapping("/{articleIds}")
    public AjaxResult remove(@PathVariable Long[] articleIds)
    {
        return toAjax(cmsArticleService.deleteCmsArticleByArticleIds(articleIds));
    }
}
