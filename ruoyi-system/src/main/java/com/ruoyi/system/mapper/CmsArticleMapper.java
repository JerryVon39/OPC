package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CmsArticle;

/**
 * CMS 文章Mapper接口
 */
public interface CmsArticleMapper
{
    /** 后台文章列表（LEFT JOIN 栏目，支持标题/栏目/状态筛选，置顶优先、发布时间倒序） */
    public List<CmsArticle> selectCmsArticleList(CmsArticle cmsArticle);

    /** 前台公开文章列表（仅已发布，置顶优先、发布时间倒序；不含正文正文体，防列表传输过大） */
    public List<CmsArticle> selectPublicArticleList(CmsArticle cmsArticle);

    /** 按ID查询文章（含正文） */
    public CmsArticle selectCmsArticleByArticleId(Long articleId);

    /** 浏览量 +1（前台详情自增） */
    public int increaseArticleViews(Long articleId);

    /** 新增文章 */
    public int insertCmsArticle(CmsArticle cmsArticle);

    /** 修改文章 */
    public int updateCmsArticle(CmsArticle cmsArticle);

    /** 状态切换（0已发布 1草稿 2已下线；置为已发布且无发布时间时自动补写） */
    public int updateCmsArticleStatus(CmsArticle cmsArticle);

    /** 发布：置为已发布，首次发布写入发布时间 */
    public int publishCmsArticle(CmsArticle cmsArticle);

    /** 批量删除文章 */
    public int deleteCmsArticleByArticleIds(Long[] articleIds);
}
