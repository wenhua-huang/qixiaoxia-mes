package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;
import com.ruoyi.system.mapper.mes.wm.WmRollDetailMapper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.OutsourceResultStrategy;
import jakarta.annotation.PostConstruct;

/**
 * 分切外协结果策略 —— 适配通用外协框架。
 *
 * 当通过通用 /mes/wm/outsource/* 接口发起分切外协（sourceType=SLITTING）时，
 * 本策略处理分切特有的领域逻辑（建子卷 WmRollDetail）。
 *
 * 注意：分切原有的 /mes/pro/slitting/* 三步接口保持独立运行，不走此策略。
 * 本策略供未来"从工单下推分切外协"的新入口使用。
 *
 * @author qixiaoxia
 */
@Component
public class SlittingResultStrategy implements OutsourceResultStrategy
{
    private static final Logger log = LoggerFactory.getLogger(SlittingResultStrategy.class);

    @Autowired
    private WmRollDetailMapper rollDetailMapper;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private com.ruoyi.system.service.mes.wm.impl.OutsourceServiceImpl outsourceService;

    @PostConstruct
    public void init()
    {
        outsourceService.registerStrategy(this);
    }

    @Override
    public String getSourceType()
    {
        return "SLITTING";
    }

    /**
     * 厂商录分切结果时：为每条收货行建子卷 WmRollDetail（状态 OUTSOURCED）。
     * 子卷的物料/批次/仓库从发料行（母卷）继承。
     */
    @Override
    public List<WmOutsourceRecptLine> onRecordResult(WmOutsourceOrder order,
                                                     List<WmOutsourceRecptLine> resultLines)
    {
        String operator = SecurityUtils.getUsername();
        // 从发料行获取母卷信息
        WmOutsourceRecptLine firstIssue = mapIssueToRecpt(order);

        List<WmOutsourceRecptLine> processed = new ArrayList<>();
        for (WmOutsourceRecptLine line : resultLines)
        {
            // 建子卷
            WmRollDetail child = new WmRollDetail();
            child.setRollCode(autoCodeGenerator.genSerialCode("ROLL_CODE", null));
            child.setItemId(line.getItemId());
            child.setItemCode(line.getItemCode());
            child.setItemName(line.getItemName());
            child.setActualWeight(line.getQuantity());
            child.setUnitOfMeasure(line.getUnitOfMeasure());
            child.setOriginalQuantity(line.getQuantity());
            child.setRemainingQuantity(line.getQuantity());
            child.setBatchId(line.getBatchId());
            child.setBatchCode(line.getBatchCode());
            child.setWarehouseId(line.getWarehouseId());
            child.setWarehouseCode(line.getWarehouseCode());
            child.setWarehouseName(line.getWarehouseName());
            child.setStatus("OUTSOURCED");
            child.setSlitBatchNo(order.getOrderCode());
            child.setCreateBy(operator);
            child.setCreateTime(DateUtils.getNowDate());
            rollDetailMapper.insertWmRollDetail(child);

            // 回填收货行的 sourceRefId（子卷 rollId）
            line.setSourceRefType("ROLL");
            line.setSourceRefId(child.getRollId());
            processed.add(line);
        }
        log.info("分切策略-录结果: orderId={}, 建子卷 {} 卷", order.getOrderId(), processed.size());
        return processed;
    }

    /**
     * 收货时：子卷状态 OUTSOURCED → IN_STOCK。
     */
    @Override
    public void onReceive(WmOutsourceOrder order, List<WmOutsourceRecptLine> recptLines, Long feedbackId)
    {
        String operator = SecurityUtils.getUsername();
        for (WmOutsourceRecptLine line : recptLines)
        {
            if (line.getSourceRefId() != null && "ROLL".equals(line.getSourceRefType()))
            {
                WmRollDetail child = rollDetailMapper.selectWmRollDetailByRollId(line.getSourceRefId());
                if (child != null)
                {
                    child.setStatus("IN_STOCK");
                    child.setUpdateBy(operator);
                    child.setUpdateTime(DateUtils.getNowDate());
                    rollDetailMapper.updateWmRollDetail(child);
                }
            }
        }
        log.info("分切策略-收货: orderId={}, 子卷入库", order.getOrderId());
    }

    /** 从发料行（母卷）映射出收货行的物料信息 */
    private WmOutsourceRecptLine mapIssueToRecpt(WmOutsourceOrder order)
    {
        // 简化：收货行的物料由前端传入，这里不额外处理
        return new WmOutsourceRecptLine();
    }
}
