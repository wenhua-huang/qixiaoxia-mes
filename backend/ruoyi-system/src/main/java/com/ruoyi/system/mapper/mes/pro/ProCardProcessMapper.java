package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;

public interface ProCardProcessMapper {
    ProCardProcess selectProCardProcessByRecordId(Long recordId);
    List<ProCardProcess> selectProCardProcessList(ProCardProcess e);
    int insertProCardProcess(ProCardProcess e);
    int updateProCardProcess(ProCardProcess e);
    int deleteProCardProcessByRecordId(Long recordId);
    int deleteProCardProcessByRecordIds(Long[] recordIds);
}
