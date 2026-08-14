package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysBanner;

/**
 * 前台轮播图Mapper接口
 */
public interface SysBannerMapper
{
    public SysBanner selectSysBannerByBannerId(Long bannerId);

    public List<SysBanner> selectSysBannerList(SysBanner sysBanner);

    public int insertSysBanner(SysBanner sysBanner);

    public int updateSysBanner(SysBanner sysBanner);

    public int deleteSysBannerByBannerIds(Long[] bannerIds);
}
