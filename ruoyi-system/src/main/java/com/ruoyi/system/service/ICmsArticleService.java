package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CmsArticle;
import com.ruoyi.system.domain.CmsArticleHistory;

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

    /** 公开详情（后台预览模式 preview=true：跳过"仅已发布"校验、不计浏览量，供编辑页 iframe 预览草稿/下线文章） */
    public CmsArticle selectPublicArticleDetail(Long articleId, boolean preview);

    /** 新增文章（状态为已发布且未填发布时间时自动写入当前时间） */
    public int insertCmsArticle(CmsArticle cmsArticle);

    /** 修改文章（状态切为已发布且无发布时间时自动补写） */
    public int updateCmsArticle(CmsArticle cmsArticle);

    /** 删除文章（软删除：移入回收站，del_flag='2'，可恢复） */
    public int deleteCmsArticleByArticleIds(Long[] articleIds);

    /** 回收站列表（del_flag='2'） */
    public List<CmsArticle> selectRecycleArticleList(CmsArticle cmsArticle);

    /** 恢复回收站文章（del_flag 置 '0'） */
    public int restoreCmsArticleByArticleIds(Long[] articleIds);

    /** 永久删除回收站文章（物理删除，不可恢复） */
    public int purgeCmsArticleByArticleIds(Long[] articleIds);

    /** 批量置顶/取消置顶 */
    public int batchTop(Long[] articleIds, String isTop);

    /** 批量状态切换（置为已发布需要发布权限，与单条一致） */
    public int batchChangeStatus(Long[] articleIds, String status);

    /** 批量排序（逐条更新 sort） */
    public int batchSort(List<CmsArticle> list);

    /** 文章历史列表（version 倒序，最多 20 版） */
    public java.util.List<CmsArticleHistory> selectHistoryByArticleId(Long articleId);

    /** 回滚到指定版本（取该版写入主表，version+1 并记新历史） */
    public int rollbackArticle(Long articleId, Long version);

    /** 状态切换：0已发布 1草稿 2已下线（置为已发布且无发布时间时自动补写） */
    public int changeArticleStatus(Long articleId, String status);

    /** 发布：草稿/已下线 → 已发布，首次发布写入发布时间 */
    public int publishArticle(Long articleId);

    /** 2：一键复制——克隆为草稿（标题加"-副本"，清发布信息/浏览量/置顶），返回新文章ID */
    public Long copyCmsArticle(Long articleId);

    /** 6：批量移动栏目（多篇文章改 categoryId） */
    public int batchMoveCategory(Long[] articleIds, Long categoryId);

    /** 5：浏览量报表——Top20 + 栏目分布 + 近30天趋势 */
    public java.util.Map<String, Object> selectStats();
}
