package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmOutsourceIssueLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;

/**
 * 外协发料/收货行 Mapper
 * @author qixiaoxia
 */
public interface WmOutsourceLineMapper
{
    List<WmOutsourceIssueLine> selectIssueLinesByOrderId(Long orderId);

    int insertIssueLine(WmOutsourceIssueLine line);

    int deleteIssueLinesByOrderId(Long orderId);

    List<WmOutsourceRecptLine> selectRecptLinesByOrderId(Long orderId);

    int insertRecptLine(WmOutsourceRecptLine line);

    /** 写回收货行批次信息（收货时生成成品批次后回写） */
    int updateRecptLine(WmOutsourceRecptLine line);
}
