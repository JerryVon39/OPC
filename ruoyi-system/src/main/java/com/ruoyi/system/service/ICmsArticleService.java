package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CmsArticle;

/**
 * CMS 文章Service接口
 */
public interface ICmsArticleService
{
    /** 后台文章列表 */
    public List<CmsArticle> selectCmsArticleList(CmsArticle cmsArticle);

    /** 前台公开文章列表（仅已发布，置顶优先、发布时间倒序） */
    public List<CmsArticle> selectPublicArticleList(CmsArticle cmsArticle);

    /** 按ID查询文章（后台编辑回显用） */
    public CmsArticle selectCmsArticleByArticleId(Long articleId);

    /** 前台文章详情：先浏览量+1 再返回文章 */
    public CmsArticle selectPublicArticleDetail(Long articleId);

    /** 新增文章（状态为已发布且未填发布时间时自动写入当前时间） */
    public int insertCmsArticle(CmsArticle cmsArticle);

    /** 修改文章（状态切为已发布且无发布时间时自动补写） */
    public int updateCmsArticle(CmsArticle cmsArticle);

    /** 批量删除文章 */
    public int deleteCmsArticleByArticleIds(Long[] articleIds);

    /** 状态切换：0已发布 1草稿 2已下线（置为已发布且无发布时间时自动补写） */
    public int changeArticleStatus(Long articleId, String status);

    /** 发布：草稿/已下线 → 已发布，首次发布写入发布时间 */
    public int publishArticle(Long articleId);
}
