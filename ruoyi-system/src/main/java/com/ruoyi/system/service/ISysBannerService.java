package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysBanner;

/**
 * 前台轮播图Service接口
 */
public interface ISysBannerService
{
    public SysBanner selectSysBannerByBannerId(Long bannerId);

    public List<SysBanner> selectSysBannerList(SysBanner sysBanner);

    public int insertSysBanner(SysBanner sysBanner);

    public int updateSysBanner(SysBanner sysBanner);

    public int deleteSysBannerByBannerIds(Long[] bannerIds);
}
