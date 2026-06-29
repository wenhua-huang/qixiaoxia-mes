package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProChangeover;

/**
 * 工序换型时间Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public interface ProChangeoverMapper
{
    ProChangeover selectProChangeoverById(Long id);

    List<ProChangeover> selectProChangeoverList(ProChangeover proChangeover);

    /**
     * 查询换型时间（排产算法专用，按优先级匹配）
     */
    ProChangeover selectChangeover(ProChangeover query);

    int insertProChangeover(ProChangeover proChangeover);

    int updateProChangeover(ProChangeover proChangeover);

    int deleteProChangeoverById(Long id);

    int deleteProChangeoverByIds(Long[] ids);
}
