package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CmsBlock;
import com.ruoyi.system.domain.CmsBlockHistory;

/**
 * CMS 区块Mapper接口
 */
public interface CmsBlockMapper
{
    /** 后台区块列表（按 pageKey/blockKey 筛选，sort 升序） */
    public List<CmsBlock> selectCmsBlockList(CmsBlock cmsBlock);

    /** 前台公开区块列表（仅 visible='0'，sort 升序） */
    public List<CmsBlock> selectPublicBlockList(String pageKey);

    /** 按ID查询区块 */
    public CmsBlock selectCmsBlockByBlockId(Long blockId);

    /** 按区块键查询（种子/去重用） */
    public CmsBlock selectCmsBlockByBlockKey(String blockKey);

    /** 新增区块 */
    public int insertCmsBlock(CmsBlock cmsBlock);

    /** 修改区块（version 由 service 置位） */
    public int updateCmsBlock(CmsBlock cmsBlock);

    /** 删除区块 */
    public int deleteCmsBlockByBlockIds(Long[] blockIds);

    /** 历史：按区块查（version 倒序，limit 20） */
    public List<CmsBlockHistory> selectHistoryByBlockId(Long blockId);

    /** 历史：按区块+版本查（回滚取数） */
    public CmsBlockHistory selectHistoryByVersion(@Param("blockId") Long blockId, @Param("version") Long version);

    /** 历史：写入一版 */
    public int insertHistory(CmsBlockHistory history);

    /** 历史：清理该区块超限最旧版本（保留最新 keep 条） */
    public int trimHistory(@Param("blockId") Long blockId, @Param("keep") int keep);

    /** 最近编辑区块（工作台"最近编辑"列表） */
    public List<CmsBlock> selectRecentBlocks(int limit);
}
