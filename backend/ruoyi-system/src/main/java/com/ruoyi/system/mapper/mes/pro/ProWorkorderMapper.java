package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import java.math.BigDecimal;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;

public interface ProWorkorderMapper {
    ProWorkorder selectProWorkorderByWorkorderId(Long id);
    List<ProWorkorder> selectProWorkorderList(ProWorkorder w);
    ProWorkorder selectProWorkorderByWorkorderCode(String code);
    int insertProWorkorder(ProWorkorder w);
    int updateProWorkorder(ProWorkorder w);
    int deleteProWorkorderByWorkorderId(Long id);
    int deleteProWorkorderByWorkorderIds(Long[] ids);
    /** 审核报工时原子增量更新工单已生产数量 */
    int addQuantityProduced(@org.apache.ibatis.annotations.Param("workorderId") Long workorderId,
            @org.apache.ibatis.annotations.Param("deltaProduced") BigDecimal deltaProduced);
}
