package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;

public interface ProWorkorderParamMapper {
    ProWorkorderParam selectProWorkorderParamByRecordId(Long id);
    List<ProWorkorderParam> selectProWorkorderParamList(ProWorkorderParam w);
    List<ProWorkorderParam> selectProWorkorderParamByWorkorderId(Long workorderId);
    int insertProWorkorderParam(ProWorkorderParam w);
    int updateProWorkorderParam(ProWorkorderParam w);
    int deleteProWorkorderParamByRecordId(Long id);
    int deleteProWorkorderParamByRecordIds(Long[] ids);
    int deleteProWorkorderParamByWorkorderId(Long workorderId);
}
