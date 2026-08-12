package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProSlittingRecord;

/**
 * 分切作业记录 Mapper
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
public interface ProSlittingRecordMapper {

    ProSlittingRecord selectProSlittingRecordBySlitId(Long slitId);

    List<ProSlittingRecord> selectProSlittingRecordList(ProSlittingRecord record);

    int insertProSlittingRecord(ProSlittingRecord record);

    int updateProSlittingRecord(ProSlittingRecord record);

    int deleteProSlittingRecordBySlitId(Long slitId);

    int deleteProSlittingRecordBySlitIds(Long[] slitIds);
}
