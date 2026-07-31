package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProSlittingRecord;
import com.ruoyi.system.domain.mes.pro.SlittingRequest;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;

/**
 * 分切作业 Service（库存驱动 + 报工自动建卷）
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
public interface IProSlittingService {

    /**
     * 执行分切作业（原子操作：领料出库 + 自动建母卷/子卷 + 库存事务 + 物料追溯 + 报工）
     *
     * @param request 分切方案（领料物料/仓库/数量 + 子卷规格 + 纸边）
     * @return 分切记录（含子卷列表）
     */
    ProSlittingRecord executeSlitting(SlittingRequest request);

    /** 查询物料在库库存（供前端选领料物料时展示可用批次） */
    List<WmMaterialStock> listAvailableStock(Long itemId);

    /** 分切记录列表 */
    List<ProSlittingRecord> selectList(ProSlittingRecord query);

    /** 分切记录详情（含子卷列表） */
    ProSlittingRecord selectBySlitId(Long slitId);
}
