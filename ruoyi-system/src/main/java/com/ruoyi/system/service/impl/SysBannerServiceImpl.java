package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysBanner;
import com.ruoyi.system.mapper.SysBannerMapper;
import com.ruoyi.system.service.ISysBannerService;

/**
 * 前台轮播图Service业务层处理
 */
@Service
public class SysBannerServiceImpl implements ISysBannerService
{
    @Autowired
    private SysBannerMapper sysBannerMapper;

    @Override
    public SysBanner selectSysBannerByBannerId(Long bannerId)
    {
        return sysBannerMapper.selectSysBannerByBannerId(bannerId);
    }

    @Override
    public List<SysBanner> selectSysBannerList(SysBanner sysBanner)
    {
        return sysBannerMapper.selectSysBannerList(sysBanner);
    }

    @Override
    public int insertSysBanner(SysBanner sysBanner)
    {
        return sysBannerMapper.insertSysBanner(sysBanner);
    }

    @Override
    public int updateSysBanner(SysBanner sysBanner)
    {
        return sysBannerMapper.updateSysBanner(sysBanner);
    }

    @Override
    public int deleteSysBannerByBannerIds(Long[] bannerIds)
    {
        return sysBannerMapper.deleteSysBannerByBannerIds(bannerIds);
    }
}
