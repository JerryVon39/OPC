package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CmsBlock;
import com.ruoyi.system.domain.CmsBlockHistory;

/**
 * CMS 区块Service接口
 */
public interface ICmsBlockService
{
    /** 后台区块列表 */
    public List<CmsBlock> selectCmsBlockList(CmsBlock cmsBlock);

    /** 前台公开区块列表（仅 visible='0'） */
    public List<CmsBlock> selectPublicBlockList(String pageKey);

    /** 按ID查询区块 */
    public CmsBlock selectCmsBlockByBlockId(Long blockId);

    /** 新增区块（version=1） */
    public int insertCmsBlock(CmsBlock cmsBlock);

    /** 修改区块：保存前自动写历史一版，version+1，历史超 20 版删最旧 */
    public int updateCmsBlock(CmsBlock cmsBlock);

    /** 删除区块（含其历史） */
    public int deleteCmsBlockByBlockIds(Long[] blockIds);

    /** 内容区块上下移（相邻 sort 交换，仅模板化区块参与排序） */
    public int moveCmsBlock(Long blockId, String dir);

    /** 区块历史列表（version 倒序） */
    public List<CmsBlockHistory> selectHistoryByBlockId(Long blockId);

    /** 回滚到指定版本：取该版写入主表，version+1 并记新历史 */
    public int rollbackCmsBlock(Long blockId, Long version);
}
