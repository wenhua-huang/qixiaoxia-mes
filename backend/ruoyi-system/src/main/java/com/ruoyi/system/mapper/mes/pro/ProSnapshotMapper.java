package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProSnapshot;

public interface ProSnapshotMapper
{
    ProSnapshot selectProSnapshotById(Long id);
    List<ProSnapshot> selectProSnapshotList(ProSnapshot query);
    int insertProSnapshot(ProSnapshot snapshot);
    int updateProSnapshot(ProSnapshot snapshot);
    int deleteProSnapshotById(Long id);
    int deleteProSnapshotByIds(Long[] ids);
}
