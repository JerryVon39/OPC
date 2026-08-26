package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.CmsArticle;
import com.ruoyi.system.domain.CmsArticleHistory;
import com.ruoyi.system.mapper.CmsArticleMapper;
import com.ruoyi.system.service.ICmsArticleService;
import com.ruoyi.system.service.StatisticsService;

/**
 * CMS 文章Service业务层处理
 */
@Service
@Transactional
public class CmsArticleServiceImpl implements ICmsArticleService
{
    @Autowired
    private CmsArticleMapper cmsArticleMapper;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private RedisCache redisCache;

    @Override
    public CmsArticle selectCmsArticleByArticleId(Long articleId)
    {
        return cmsArticleMapper.selectCmsArticleByArticleId(articleId);
    }

    @Override
    public List<CmsArticle> selectCmsArticleList(CmsArticle cmsArticle)
    {
        return cmsArticleMapper.selectCmsArticleList(cmsArticle);
    }

    @Override
    public List<CmsArticle> selectPublicArticleList(CmsArticle cmsArticle)
    {
        return cmsArticleMapper.selectPublicArticleList(cmsArticle);
    }

    @Override
    public CmsArticle selectPublicArticleDetail(Long articleId)
    {
        return selectPublicArticleDetail(articleId, false);
    }

    @Override
    public CmsArticle selectPublicArticleDetail(Long articleId, boolean preview)
    {
        CmsArticle article = cmsArticleMapper.selectCmsArticleByArticleId(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        // 仅已发布文章可公开访问（草稿/已下线对前台不可见，防止未发布内容泄露）
        // 预览模式（preview=true）跳过该校验：后台编辑页 iframe 预览专用，仍不可见未删除之外的文章
        if (!preview && !"0".equals(article.getStatus()))
        {
            throw new ServiceException("文章未发布");
        }
        // 浏览量自增（防刷：同一 IP + 文章 10 分钟内只计一次；Redis 异常降级为照常自增）
        // 预览模式不计浏览量（编辑页每次保存都会刷新 iframe，避免刷虚）
        if (!preview && viewNotCounted(articleId))
        {
            try
            {
                cmsArticleMapper.increaseArticleViews(articleId);
                article.setViews(article.getViews() == null ? 1 : article.getViews() + 1);
            }
            catch (Exception e)
            {
                // 浏览量自增失败不阻断阅读
            }
        }
        return article;
    }

    @Override
    public int insertCmsArticle(CmsArticle cmsArticle)
    {
        if (cmsArticle == null || cmsArticle.getTitle() == null || cmsArticle.getTitle().trim().isEmpty())
        {
            throw new ServiceException("文章标题不能为空");
        }
        // 新建即为已发布状态时，自动写入发布时间（前台按发布时间倒序展示）
        if ("0".equals(cmsArticle.getStatus()) && cmsArticle.getPublishTime() == null)
        {
            cmsArticle.setPublishTime(new Date());
        }
        int rows = cmsArticleMapper.insertCmsArticle(cmsArticle);
        statisticsService.evictAll(); // 文章计数变了：失效工作台统计缓存
        return rows;
    }

    @Override
    public int updateCmsArticle(CmsArticle cmsArticle)
    {
        if (cmsArticle == null || cmsArticle.getArticleId() == null)
        {
            throw new ServiceException("文章ID不能为空");
        }
        CmsArticle existing = cmsArticleMapper.selectCmsArticleByArticleId(cmsArticle.getArticleId());
        if (existing == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        // 草稿/下线 → 已发布属于发布动作：必须持有发布权限（防仅 edit 权限者经编辑弹窗旁路发布）
        checkPublishPerm(existing.getStatus(), cmsArticle.getStatus());
        // 状态切为已发布且尚无发布时间时自动补写（草稿编辑后直接发布场景）
        if ("0".equals(cmsArticle.getStatus()) && cmsArticle.getPublishTime() == null)
        {
            cmsArticle.setPublishTime(new Date());
        }
        // 保存前写当前版本进历史（回滚基线），再更新主表 version+1（批次 A：文章版本历史）
        saveHistory(existing);
        cmsArticle.setVersion((existing.getVersion() == null ? 0L : existing.getVersion()) + 1);
        int rows = cmsArticleMapper.updateCmsArticle(cmsArticle);
        cmsArticleMapper.trimArticleHistory(cmsArticle.getArticleId(), HISTORY_KEEP);
        statisticsService.evictAll(); // 状态/内容可能影响统计
        return rows;
    }

    /** 每篇文章历史版本上限 */
    private static final int HISTORY_KEEP = 20;

    /** 把文章当前版本写入历史表（回滚基线） */
    private void saveHistory(CmsArticle a)
    {
        if (a == null || a.getArticleId() == null)
        {
            return;
        }
        CmsArticleHistory h = new CmsArticleHistory();
        h.setArticleId(a.getArticleId());
        h.setVersion(a.getVersion() == null ? 1L : a.getVersion());
        h.setCategoryId(a.getCategoryId());
        h.setTitle(a.getTitle());
        h.setSummary(a.getSummary());
        h.setContent(a.getContent());
        h.setCover(a.getCover());
        h.setAuthor(a.getAuthor());
        h.setIsTop(a.getIsTop());
        h.setStatus(a.getStatus());
        h.setSort(a.getSort());
        h.setAttachment(a.getAttachment());
        h.setKeywords(a.getKeywords());
        h.setDescription(a.getDescription());
        h.setPublishTime(a.getPublishTime());
        h.setUpdateBy(a.getUpdateBy());
        h.setUpdateTime(a.getUpdateTime() == null ? new Date() : a.getUpdateTime());
        cmsArticleMapper.insertArticleHistory(h);
    }

    @Override
    public java.util.List<CmsArticleHistory> selectHistoryByArticleId(Long articleId)
    {
        if (articleId == null)
        {
            return java.util.Collections.emptyList();
        }
        return cmsArticleMapper.selectHistoryByArticleId(articleId);
    }

    @Override
    public int rollbackArticle(Long articleId, Long version)
    {
        if (articleId == null || version == null)
        {
            throw new ServiceException("回滚参数不合法");
        }
        CmsArticle existing = cmsArticleMapper.selectCmsArticleByArticleId(articleId);
        if (existing == null)
        {
            throw new ServiceException("文章不存在");
        }
        CmsArticleHistory hist = cmsArticleMapper.selectHistoryByVersion(articleId, version);
        if (hist == null)
        {
            throw new ServiceException("历史版本不存在（版本 " + version + "）");
        }
        // 取该版写入主表，version+1 并记新历史（回滚本身也是一次可回滚的操作）
        CmsArticle target = new CmsArticle();
        target.setArticleId(articleId);
        target.setCategoryId(hist.getCategoryId());
        target.setTitle(hist.getTitle());
        target.setSummary(hist.getSummary());
        target.setContent(hist.getContent());
        target.setCover(hist.getCover());
        target.setAuthor(hist.getAuthor());
        target.setIsTop(hist.getIsTop());
        target.setStatus(hist.getStatus());
        target.setSort(hist.getSort());
        target.setAttachment(hist.getAttachment());
        target.setKeywords(hist.getKeywords());
        target.setDescription(hist.getDescription());
        target.setPublishTime(hist.getPublishTime());
        target.setVersion((existing.getVersion() == null ? 0L : existing.getVersion()) + 1);
        target.setUpdateBy(operator());
        target.setUpdateTime(new Date());
        int rows = cmsArticleMapper.updateCmsArticle(target);
        if (rows > 0)
        {
            saveHistory(target);
            cmsArticleMapper.trimArticleHistory(articleId, HISTORY_KEEP);
            statisticsService.evictAll();
        }
        return rows;
    }

    /** 定时任务：清理回收站超 30 天文章（每日执行，RuoYi 定时任务调用） */
    public void purgeRecycleBinExpired()
    {
        int rows = cmsArticleMapper.purgeExpiredRecycle();
        if (rows > 0)
        {
            statisticsService.evictAll();
        }
    }

    /** 当前登录用户名（未登录容错为空串） */
    private String operator()
    {
        try { return com.ruoyi.common.utils.SecurityUtils.getUsername(); }
        catch (Exception e) { return ""; }
    }

    @Override
    public int deleteCmsArticleByArticleIds(Long[] articleIds)
    {
        if (articleIds == null || articleIds.length == 0)
        {
            return 0;
        }
        for (Long articleId : articleIds)
        {
            if (articleId == null || cmsArticleMapper.selectCmsArticleByArticleId(articleId) == null)
            {
                throw new ServiceException("部分文章不存在或已删除");
            }
        }
        // 软删除（两态，对齐 book）：数据保留在原表，del_flag 置 '2'，后台回收站可恢复/永久删除
        int rows = cmsArticleMapper.softDeleteCmsArticleByArticleIds(articleIds, operator(), new Date());
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public List<CmsArticle> selectRecycleArticleList(CmsArticle cmsArticle)
    {
        if (cmsArticle == null)
        {
            cmsArticle = new CmsArticle();
        }
        cmsArticle.setDelFlag("2");
        return cmsArticleMapper.selectCmsArticleList(cmsArticle);
    }

    @Override
    public int restoreCmsArticleByArticleIds(Long[] articleIds)
    {
        if (articleIds == null || articleIds.length == 0)
        {
            return 0;
        }
        int rows = cmsArticleMapper.restoreCmsArticleByArticleIds(articleIds);
        if (rows == 0)
        {
            throw new ServiceException("部分文章不在回收站中，无法恢复");
        }
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public int purgeCmsArticleByArticleIds(Long[] articleIds)
    {
        if (articleIds == null || articleIds.length == 0)
        {
            return 0;
        }
        int rows = cmsArticleMapper.purgeCmsArticleByArticleIds(articleIds);
        if (rows == 0)
        {
            throw new ServiceException("部分文章不在回收站中，无法彻底删除");
        }
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public int batchTop(Long[] articleIds, String isTop)
    {
        if (articleIds == null || articleIds.length == 0)
        {
            return 0;
        }
        if (isTop == null || (!"0".equals(isTop) && !"1".equals(isTop)))
        {
            throw new ServiceException("置顶参数不合法（0普通 1置顶）");
        }
        return cmsArticleMapper.batchUpdateTop(articleIds, isTop);
    }

    @Override
    public int batchChangeStatus(Long[] articleIds, String status)
    {
        if (articleIds == null || articleIds.length == 0)
        {
            return 0;
        }
        if (status == null || (!"0".equals(status) && !"1".equals(status) && !"2".equals(status)))
        {
            throw new ServiceException("状态参数不合法（0已发布 1草稿 2已下线）");
        }
        // 批量置为已发布同样需要发布权限（与单条 changeArticleStatus 同一守卫，防仅 edit 权限者旁路发布）
        if ("0".equals(status))
        {
            if (!SecurityUtils.getLoginUser().getPermissions().contains("system:cms:publish"))
            {
                throw new ServiceException("无发布权限：批量发布需要 system:cms:publish 权限");
            }
        }
        int rows = cmsArticleMapper.batchUpdateStatus(articleIds, status);
        statisticsService.evictAll();
        return rows;
    }

    @Override
    public int batchSort(List<CmsArticle> list)
    {
        if (list == null || list.isEmpty())
        {
            return 0;
        }
        for (CmsArticle a : list)
        {
            if (a == null || a.getArticleId() == null)
            {
                throw new ServiceException("排序参数不合法：缺少文章ID");
            }
        }
        return cmsArticleMapper.batchUpdateSort(list);
    }

    /**
     * 浏览量防刷：同一 IP + 文章 10 分钟内只计一次。
     * 返回 true = 本次应计数（首次访问或 Redis 不可用降级）；false = 命中防刷窗口跳过。
     */
    private boolean viewNotCounted(Long articleId)
    {
        try
        {
            String ip = IpUtils.getIpAddr(ServletUtils.getRequest());
            String key = "cms:view:" + ip + ":" + articleId;
            if (Boolean.TRUE.equals(redisCache.hasKey(key)))
            {
                return false;
            }
            redisCache.setCacheObject(key, 1, 10, TimeUnit.MINUTES);
            return true;
        }
        catch (Exception e)
        {
            return true; // Redis 异常：降级为照常自增，不阻断阅读
        }
    }

    @Override
    public int changeArticleStatus(Long articleId, String status)
    {
        if (articleId == null)
        {
            throw new ServiceException("文章ID不能为空");
        }
        if (status == null || (!"0".equals(status) && !"1".equals(status) && !"2".equals(status)))
        {
            throw new ServiceException("状态参数不合法（0已发布 1草稿 2已下线）");
        }
        CmsArticle existing = cmsArticleMapper.selectCmsArticleByArticleId(articleId);
        if (existing == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        // 草稿/下线 → 已发布属于发布动作：必须持有发布权限（与 updateCmsArticle 同一守卫）
        checkPublishPerm(existing.getStatus(), status);
        CmsArticle article = new CmsArticle();
        article.setArticleId(articleId);
        article.setStatus(status);
        int rows = cmsArticleMapper.updateCmsArticleStatus(article);
        statisticsService.evictAll();
        return rows;
    }

    /**
     * 发布守卫：状态切为「已发布」且原状态非已发布时，要求持有 system:cms:publish 权限。
     * （此前 PUT /cms 与 PUT /cms/changeStatus 仅需 edit 权限即可把文章改为已发布，
     *   系统:cms:publish 权限点形同虚设——H5 修复）
     */
    private void checkPublishPerm(String oldStatus, String newStatus)
    {
        if (!"0".equals(newStatus) || "0".equals(oldStatus))
        {
            return;
        }
        if (!SecurityUtils.getLoginUser().getPermissions().contains("system:cms:publish"))
        {
            throw new ServiceException("无发布权限：将文章从草稿/下线改为已发布需要 system:cms:publish 权限");
        }
    }

    @Override
    public int publishArticle(Long articleId)
    {
        if (articleId == null)
        {
            throw new ServiceException("文章ID不能为空");
        }
        if (cmsArticleMapper.selectCmsArticleByArticleId(articleId) == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        CmsArticle article = new CmsArticle();
        article.setArticleId(articleId);
        int rows = cmsArticleMapper.publishCmsArticle(article);
        statisticsService.evictAll();
        return rows;
    }
}
