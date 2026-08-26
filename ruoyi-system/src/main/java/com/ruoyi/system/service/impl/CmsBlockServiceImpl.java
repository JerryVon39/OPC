package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.CmsBlock;
import com.ruoyi.system.domain.CmsBlockHistory;
import com.ruoyi.system.mapper.CmsBlockMapper;
import com.ruoyi.system.service.ICmsBlockService;

/**
 * CMS 区块Service业务层处理（保存自动写历史、回滚、每区块 20 版上限）
 */
@Service
@Transactional
public class CmsBlockServiceImpl implements ICmsBlockService
{
    /** 每区块历史版本上限 */
    private static final int HISTORY_KEEP = 20;

    @Autowired
    private CmsBlockMapper cmsBlockMapper;

    @Override
    public List<CmsBlock> selectCmsBlockList(CmsBlock cmsBlock)
    {
        return cmsBlockMapper.selectCmsBlockList(cmsBlock);
    }

    @Override
    public List<CmsBlock> selectPublicBlockList(String pageKey)
    {
        return cmsBlockMapper.selectPublicBlockList(pageKey);
    }

    @Override
    public CmsBlock selectCmsBlockByBlockId(Long blockId)
    {
        return cmsBlockMapper.selectCmsBlockByBlockId(blockId);
    }

    @Override
    public int insertCmsBlock(CmsBlock cmsBlock)
    {
        if (cmsBlock == null || cmsBlock.getBlockKey() == null || cmsBlock.getBlockKey().trim().isEmpty())
        {
            throw new ServiceException("区块键不能为空");
        }
        if (cmsBlockMapper.selectCmsBlockByBlockKey(cmsBlock.getBlockKey()) != null)
        {
            throw new ServiceException("区块键已存在：" + cmsBlock.getBlockKey());
        }
        cmsBlock.setVersion(1L);
        cmsBlock.setUpdateBy(operator());
        cmsBlock.setUpdateTime(new Date());
        return cmsBlockMapper.insertCmsBlock(cmsBlock);
    }

    @Override
    public int updateCmsBlock(CmsBlock cmsBlock)
    {
        if (cmsBlock == null || cmsBlock.getBlockId() == null)
        {
            throw new ServiceException("区块ID不能为空");
        }
        CmsBlock existing = cmsBlockMapper.selectCmsBlockByBlockId(cmsBlock.getBlockId());
        if (existing == null)
        {
            throw new ServiceException("区块不存在");
        }
        // 防御：全字段为 null 时动态 SET 为空会生成非法 SQL，拒绝空更新
        if (cmsBlock.getTitle() == null && cmsBlock.getSubtitle() == null && cmsBlock.getContent() == null
            && cmsBlock.getImage() == null && cmsBlock.getLink() == null
            && cmsBlock.getTemplate() == null && cmsBlock.getConfigJson() == null
            && cmsBlock.getSort() == null && cmsBlock.getVisible() == null)
        {
            throw new ServiceException("没有要更新的内容");
        }
        // 保存前先写当前版本进历史（回滚基线），再更新主表 version+1
        CmsBlockHistory history = new CmsBlockHistory();
        history.setBlockId(existing.getBlockId());
        history.setVersion(existing.getVersion());
        history.setTitle(existing.getTitle());
        history.setSubtitle(existing.getSubtitle());
        history.setContent(existing.getContent());
        history.setImage(existing.getImage());
        history.setLink(existing.getLink());
        history.setTemplate(existing.getTemplate());
        history.setConfigJson(existing.getConfigJson());
        history.setUpdateBy(existing.getUpdateBy());
        history.setUpdateTime(existing.getUpdateTime() == null ? new Date() : existing.getUpdateTime());
        cmsBlockMapper.insertHistory(history);

        cmsBlock.setVersion((existing.getVersion() == null ? 0L : existing.getVersion()) + 1);
        cmsBlock.setUpdateBy(operator());
        cmsBlock.setUpdateTime(new Date());
        int rows = cmsBlockMapper.updateCmsBlock(cmsBlock);
        // 历史超限清理（保留最新 20 条）
        cmsBlockMapper.trimHistory(existing.getBlockId(), HISTORY_KEEP);
        return rows;
    }

    @Override
    public int moveCmsBlock(Long blockId, String dir)
    {
        if (blockId == null || (!"up".equals(dir) && !"down".equals(dir)))
        {
            throw new ServiceException("移动参数不合法");
        }
        CmsBlock current = cmsBlockMapper.selectCmsBlockByBlockId(blockId);
        if (current == null)
        {
            throw new ServiceException("区块不存在");
        }
        // 找相邻内容区块（sort 邻近），交换 sort 值（避免整体重排）
        CmsBlock neighbor = cmsBlockMapper.selectNeighborBlock(
            current.getPageKey(), current.getSort() == null ? 0L : current.getSort(), dir);
        if (neighbor == null)
        {
            return 0; // 已到边界，无可移动
        }
        Long curSort = current.getSort();
        CmsBlock a = new CmsBlock();
        a.setBlockId(current.getBlockId());
        a.setSort(neighbor.getSort());
        CmsBlock b = new CmsBlock();
        b.setBlockId(neighbor.getBlockId());
        b.setSort(curSort);
        cmsBlockMapper.updateCmsBlock(a);
        cmsBlockMapper.updateCmsBlock(b);
        return 1;
    }

    @Override
    public int deleteCmsBlockByBlockIds(Long[] blockIds)
    {
        if (blockIds == null || blockIds.length == 0)
        {
            return 0;
        }
        int rows = cmsBlockMapper.deleteCmsBlockByBlockIds(blockIds);
        // 历史随区块删除（逐块清理）
        for (Long blockId : blockIds)
        {
            if (blockId != null)
            {
                cmsBlockMapper.trimHistory(blockId, 0);
            }
        }
        return rows;
    }

    @Override
    public List<CmsBlockHistory> selectHistoryByBlockId(Long blockId)
    {
        return cmsBlockMapper.selectHistoryByBlockId(blockId);
    }

    @Override
    public int rollbackCmsBlock(Long blockId, Long version)
    {
        if (blockId == null || version == null)
        {
            throw new ServiceException("回滚参数不合法");
        }
        CmsBlock existing = cmsBlockMapper.selectCmsBlockByBlockId(blockId);
        if (existing == null)
        {
            throw new ServiceException("区块不存在");
        }
        CmsBlockHistory hist = cmsBlockMapper.selectHistoryByVersion(blockId, version);
        if (hist == null)
        {
            throw new ServiceException("历史版本不存在（版本 " + version + "）");
        }
        // 取该版写入主表，version+1 并记新历史（回滚本身也是一次可回滚的操作）
        CmsBlock target = new CmsBlock();
        target.setBlockId(blockId);
        target.setTitle(hist.getTitle());
        target.setSubtitle(hist.getSubtitle());
        target.setContent(hist.getContent());
        target.setImage(hist.getImage());
        target.setLink(hist.getLink());
        target.setTemplate(hist.getTemplate());
        target.setConfigJson(hist.getConfigJson());
        target.setVersion((existing.getVersion() == null ? 0L : existing.getVersion()) + 1);
        target.setUpdateBy(operator());
        target.setUpdateTime(new Date());
        int rows = cmsBlockMapper.updateCmsBlock(target);
        if (rows > 0)
        {
            CmsBlockHistory back = new CmsBlockHistory();
            back.setBlockId(blockId);
            back.setVersion(target.getVersion());
            back.setTitle(target.getTitle());
            back.setSubtitle(target.getSubtitle());
            back.setContent(target.getContent());
            back.setImage(target.getImage());
            back.setLink(target.getLink());
            back.setTemplate(target.getTemplate());
            back.setConfigJson(target.getConfigJson());
            back.setUpdateBy(target.getUpdateBy());
            back.setUpdateTime(target.getUpdateTime());
            cmsBlockMapper.insertHistory(back);
            cmsBlockMapper.trimHistory(blockId, HISTORY_KEEP);
        }
        return rows;
    }

    /** 当前登录用户名（未登录容错为空串） */
    private String operator()
    {
        try { return SecurityUtils.getUsername(); }
        catch (Exception e) { return ""; }
    }
}
