package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;

public interface ProWorkrecordMapper {
    ProWorkrecord selectProWorkrecordByRecordId(Long recordId);
    List<ProWorkrecord> selectProWorkrecordList(ProWorkrecord e);
    int insertProWorkrecord(ProWorkrecord e);
    int updateProWorkrecord(ProWorkrecord e);
    int deleteProWorkrecordByRecordId(Long recordId);
    int deleteProWorkrecordByRecordIds(Long[] recordIds);
}
