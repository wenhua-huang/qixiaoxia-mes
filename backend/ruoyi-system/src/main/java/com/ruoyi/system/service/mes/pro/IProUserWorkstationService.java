package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;

/**
 * ProUserWorkstationService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProUserWorkstationService
{
    public ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId);
    public List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e);
    public List<ProUserWorkstation> selectAll();
    public int insertProUserWorkstation(ProUserWorkstation e);
    public int updateProUserWorkstation(ProUserWorkstation e);
    public int deleteProUserWorkstationByRecordIds(Long[] recordIds);
    public int deleteProUserWorkstationByRecordId(Long recordId);
}
