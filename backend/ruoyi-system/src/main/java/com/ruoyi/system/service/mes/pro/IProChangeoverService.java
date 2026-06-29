package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProChangeover;

/**
 * 工序换型时间Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public interface IProChangeoverService
{
    ProChangeover selectProChangeoverById(Long id);

    List<ProChangeover> selectProChangeoverList(ProChangeover proChangeover);

    /**
     * 查询换型时间（排产算法专用）
     * 匹配优先级：精确匹配(from+to+ws) > 通用匹配(from+to) > 默认(0)
     */
    Integer getChangeoverMinutes(Long fromProcessId, Long toProcessId, Long workstationId, Long factoryId);

    int insertProChangeover(ProChangeover proChangeover);

    int updateProChangeover(ProChangeover proChangeover);

    int deleteProChangeoverByIds(Long[] ids);
}
