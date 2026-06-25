package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;

public interface ProUserWorkstationMapper {
    ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId);
    List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e);
    int insertProUserWorkstation(ProUserWorkstation e);
    int updateProUserWorkstation(ProUserWorkstation e);
    int deleteProUserWorkstationByRecordId(Long recordId);
    int deleteProUserWorkstationByRecordIds(Long[] recordIds);
}
