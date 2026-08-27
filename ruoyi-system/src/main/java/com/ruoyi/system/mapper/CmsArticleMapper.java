package com.ruoyi.system.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CmsArticle;
import com.ruoyi.system.domain.CmsArticleHistory;
import com.ruoyi.system.domain.CmsArticleHistory;

/**
 * CMS 文章Mapper接口
 */
public interface CmsArticleMapper
{
    /** 后台文章列表（LEFT JOIN 栏目，支持标题/栏目/状态/软删筛选，置顶优先、sort、发布时间倒序） */
    public List<CmsArticle> selectCmsArticleList(CmsArticle cmsArticle);

    /** 统计栏目下的有效文章数（守卫：删栏目前检查，不含已删除） */
    public int countCmsArticleByCategoryId(Long categoryId);

    /** 前台公开文章列表（仅已发布、未删除，置顶优先、sort、发布时间倒序；不含正文，防列表传输过大） */
    public List<CmsArticle> selectPublicArticleList(CmsArticle cmsArticle);

    /** 按ID查询文章（含正文，仅未删除） */
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

    /** 批量置顶/取消置顶 */
    public int batchUpdateTop(@Param("articleIds") Long[] articleIds, @Param("isTop") String isTop);

    /** 批量状态切换（0已发布 1草稿 2已下线；置为已发布且无发布时间时自动补写） */
    public int batchUpdateStatus(@Param("articleIds") Long[] articleIds, @Param("status") String status);

    /** 批量排序（逐条更新 sort） */
    public int batchUpdateSort(@Param("list") List<CmsArticle> list);

    /** 软删除（两态：del_flag 置 '2'，写入删除人/删除时间；仅未删除的可删） */
    public int softDeleteCmsArticleByArticleIds(@Param("articleIds") Long[] articleIds, @Param("deletedBy") String deletedBy, @Param("deletedTime") Date deletedTime);

    /** 恢复（del_flag 置 '0'，清空删除人/时间；仅回收站内可恢复） */
    public int restoreCmsArticleByArticleIds(@Param("articleIds") Long[] articleIds);

    /** 永久删除（物理删除；仅回收站内可彻底删除） */
    /** 定时清理：回收站中超 30 天文章永久删除 */
    public int purgeExpiredRecycle();

    public int purgeCmsArticleByArticleIds(@Param("articleIds") Long[] articleIds);

    /** 文章统计（运营工作台数据卡）：总数/今日发文/草稿/回收站 */
    public java.util.Map<String, Object> selectCmsArticleStats();

    /** 最近编辑文章（工作台"最近编辑"列表，不含正文） */
    public List<CmsArticle> selectRecentArticles(int limit);

    /** 历史：写入一版（保存前写当前版） */
    public int insertArticleHistory(CmsArticleHistory history);

    /** 历史：按文章查（version 倒序，limit 20） */
    public List<CmsArticleHistory> selectHistoryByArticleId(Long articleId);

    /** 历史：按文章+版本查（回滚取数） */
    public CmsArticleHistory selectHistoryByVersion(@Param("articleId") Long articleId, @Param("version") Long version);

    /** 历史：清理该文章超限最旧版本 */
    public int trimArticleHistory(@Param("articleId") Long articleId, @Param("keep") int keep);

    /** 6：批量移动栏目 */
    public int batchMoveCategory(@Param("articleIds") Long[] articleIds, @Param("categoryId") Long categoryId);

    /** 5：日浏览量 upsert（报表趋势） */
    public int insertDailyView(@Param("articleId") Long articleId);

    /** 5：浏览量 Top N */
    public java.util.List<java.util.Map<String, Object>> selectTopArticles(@Param("limit") int limit);

    /** 5：栏目浏览量分布 */
    public java.util.List<java.util.Map<String, Object>> selectViewsByCategory();

    /** 5：近 30 天趋势 */
    public java.util.List<java.util.Map<String, Object>> selectViewsTrend();
}
