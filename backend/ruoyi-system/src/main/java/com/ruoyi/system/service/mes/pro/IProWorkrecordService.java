package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;

/**
 * ProWorkrecordService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProWorkrecordService
{
    public ProWorkrecord selectProWorkrecordByRecordId(Long recordId);
    public List<ProWorkrecord> selectProWorkrecordList(ProWorkrecord e);
    public List<ProWorkrecord> selectAll();
    public int insertProWorkrecord(ProWorkrecord e);
    public int updateProWorkrecord(ProWorkrecord e);
    public int deleteProWorkrecordByRecordIds(Long[] recordIds);
    public int deleteProWorkrecordByRecordId(Long recordId);
}
