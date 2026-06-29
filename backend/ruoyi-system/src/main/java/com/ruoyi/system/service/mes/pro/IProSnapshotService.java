package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProSnapshot;

public interface IProSnapshotService
{
    ProSnapshot selectById(Long id);
    List<ProSnapshot> selectList(ProSnapshot query);
    int insert(ProSnapshot snapshot);
    int publish(Long id);
    int discard(Long id);
    int deleteByIds(Long[] ids);
}
