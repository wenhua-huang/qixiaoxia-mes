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

    /**
     * 仅当工单当前状态为 PRODUCING 时，原子地把状态置为 COMPLETED 并设置 finish_date。
     * 精准 UPDATE 避免 full-entity 覆盖并发写入的其它列 (Fix Finding #2 TOCTOU)。
     * 返回受影响行数：0 表示状态已变（并发路径已完工/取消），非 0 表示本次完工成功。
     */
    int completeWorkorderIfProducing(@org.apache.ibatis.annotations.Param("workorderId") Long workorderId,
            @org.apache.ibatis.annotations.Param("finishDate") java.util.Date finishDate,
            @org.apache.ibatis.annotations.Param("updateBy") String updateBy,
            @org.apache.ibatis.annotations.Param("updateTime") java.util.Date updateTime);
}
