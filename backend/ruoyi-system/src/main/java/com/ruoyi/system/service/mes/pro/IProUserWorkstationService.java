package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchResult;

/**
 * ProUserWorkstationService接口
 */
public interface IProUserWorkstationService
{
    ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId);
    List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e);
    List<ProUserWorkstation> selectAll();
    List<MdWorkstation> selectWorkstationOptions();
    UserWorkstationBatchResult batchBind(UserWorkstationBatchRequest request);
    int insertProUserWorkstation(ProUserWorkstation e);
    int updateProUserWorkstation(ProUserWorkstation e);
    int deleteProUserWorkstationByRecordIds(Long[] recordIds);
    int deleteProUserWorkstationByRecordId(Long recordId);
}
