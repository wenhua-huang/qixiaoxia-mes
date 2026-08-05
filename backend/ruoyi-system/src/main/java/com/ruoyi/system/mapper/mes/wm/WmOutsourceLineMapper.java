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

    List<WmOutsourceRecptLine> selectRecptLinesByOrderId(Long orderId);

    int insertRecptLine(WmOutsourceRecptLine line);
}
