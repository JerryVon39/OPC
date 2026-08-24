package com.ruoyi.system.mapper;

import java.util.List;

import com.ruoyi.system.domain.ReaderLoginLog;

/**
 * 读者端登录审计 Mapper
 */
public interface ReaderLoginLogMapper
{
    /** 插入审计记录 */
    int insertReaderLoginLog(ReaderLoginLog log);

    /** 后台分页查询 */
    List<ReaderLoginLog> selectReaderLoginLogList(ReaderLoginLog query);
}
