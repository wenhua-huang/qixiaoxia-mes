package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorderChange;

public interface ProWorkorderChangeMapper {
    ProWorkorderChange selectProWorkorderChangeByChangeId(Long id);
    List<ProWorkorderChange> selectProWorkorderChangeList(ProWorkorderChange w);
    List<ProWorkorderChange> selectProWorkorderChangeByWorkorderId(Long workorderId);
    int insertProWorkorderChange(ProWorkorderChange w);
    int updateProWorkorderChange(ProWorkorderChange w);
    int deleteProWorkorderChangeByChangeId(Long id);
    int deleteProWorkorderChangeByChangeIds(Long[] ids);
}
