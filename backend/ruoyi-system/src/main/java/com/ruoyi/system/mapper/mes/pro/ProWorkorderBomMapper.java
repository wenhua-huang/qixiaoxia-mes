package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;

public interface ProWorkorderBomMapper {
    ProWorkorderBom selectProWorkorderBomByLineId(Long id);
    List<ProWorkorderBom> selectProWorkorderBomList(ProWorkorderBom w);
    List<ProWorkorderBom> selectProWorkorderBomByWorkorderId(Long workorderId);
    int insertProWorkorderBom(ProWorkorderBom w);
    int updateProWorkorderBom(ProWorkorderBom w);
    int deleteProWorkorderBomByLineId(Long id);
    int deleteProWorkorderBomByLineIds(Long[] ids);
    int deleteProWorkorderBomByWorkorderId(Long workorderId);
}
