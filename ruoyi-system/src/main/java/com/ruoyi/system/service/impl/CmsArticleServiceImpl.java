package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.CmsArticle;
import com.ruoyi.system.mapper.CmsArticleMapper;
import com.ruoyi.system.service.ICmsArticleService;

/**
 * CMS 文章Service业务层处理
 */
@Service
public class CmsArticleServiceImpl implements ICmsArticleService
{
    @Autowired
    private CmsArticleMapper cmsArticleMapper;

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
        CmsArticle article = cmsArticleMapper.selectCmsArticleByArticleId(articleId);
        if (article == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        // 仅已发布文章可公开访问（草稿/已下线对前台不可见，防止未发布内容泄露）
        if (!"0".equals(article.getStatus()))
        {
            throw new ServiceException("文章未发布");
        }
        // 浏览量自增（真实阅读量，异常时不影响详情展示）
        try
        {
            cmsArticleMapper.increaseArticleViews(articleId);
            article.setViews(article.getViews() == null ? 1 : article.getViews() + 1);
        }
        catch (Exception e)
        {
            // 浏览量自增失败不阻断阅读
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
        return cmsArticleMapper.insertCmsArticle(cmsArticle);
    }

    @Override
    public int updateCmsArticle(CmsArticle cmsArticle)
    {
        if (cmsArticle == null || cmsArticle.getArticleId() == null)
        {
            throw new ServiceException("文章ID不能为空");
        }
        if (cmsArticleMapper.selectCmsArticleByArticleId(cmsArticle.getArticleId()) == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        // 状态切为已发布且尚无发布时间时自动补写（草稿编辑后直接发布场景）
        if ("0".equals(cmsArticle.getStatus()) && cmsArticle.getPublishTime() == null)
        {
            cmsArticle.setPublishTime(new Date());
        }
        return cmsArticleMapper.updateCmsArticle(cmsArticle);
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
        return cmsArticleMapper.deleteCmsArticleByArticleIds(articleIds);
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
        if (cmsArticleMapper.selectCmsArticleByArticleId(articleId) == null)
        {
            throw new ServiceException("文章不存在或已删除");
        }
        CmsArticle article = new CmsArticle();
        article.setArticleId(articleId);
        article.setStatus(status);
        return cmsArticleMapper.updateCmsArticleStatus(article);
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
        return cmsArticleMapper.publishCmsArticle(article);
    }
}
