package com.ruoyi.system.service.mes.pro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

        List<WmOutsourceRecptLine> processed = new ArrayList<>();
        for (WmOutsourceRecptLine line : resultLines)
        {
            // 建子卷
            WmRollDetail child = new WmRollDetail();
            child.setRollCode(genRollCode());
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
            // 关联母卷：分切外协建单时 sourceRefId 指向母卷 rollId，
            // 不设会导致子卷 parentRollId 为空，追溯断链（无法从子卷反查母卷）
            if (order.getSourceRefId() != null) child.setParentRollId(order.getSourceRefId());
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
     * 收货时：子卷状态 OUTSOURCED → IN_STOCK，并回写收货时解析出的成品批次
     * （录结果时 batchId 为空，批次在收货环节按厂商填写的 lot/produceDate 生成）。
     */
    @Override
    public void onReceive(WmOutsourceOrder order, List<WmOutsourceRecptLine> recptLines,
                          Long feedbackId, Map<Long, Long> stockIdByLineId)
    {
        String operator = SecurityUtils.getUsername();
        int updated = 0;
        for (WmOutsourceRecptLine line : recptLines)
        {
            if (line.getSourceRefId() != null && "ROLL".equals(line.getSourceRefType()))
            {
                WmRollDetail child = rollDetailMapper.selectWmRollDetailByRollId(line.getSourceRefId());
                if (child != null)
                {
                    child.setStatus("IN_STOCK");
                    // 回写成品批次（收货环节 resolveRecptBatch 生成）
                    if (line.getBatchId() != null) child.setBatchId(line.getBatchId());
                    if (line.getBatchCode() != null) child.setBatchCode(line.getBatchCode());
                    // 回填库存ID（入库事务生成），持久化避免追溯断链
                    Long stockId = stockIdByLineId != null ? stockIdByLineId.get(line.getLineId()) : null;
                    if (stockId != null) child.setMaterialStockId(stockId);
                    child.setUpdateBy(operator);
                    child.setUpdateTime(DateUtils.getNowDate());
                    rollDetailMapper.updateWmRollDetail(child);
                    updated++;
                }
            }
        }
        log.info("分切策略-收货: orderId={}, 子卷入库 {} 卷", order.getOrderId(), updated);
    }

    /**
     * 生成子卷编码，自动编码规则不可用时以「RL + 时间戳 + 4 位随机数」兜底
     * （DB 唯一约束最终兜底）。
     */
    private String genRollCode()
    {
        if (autoCodeGenerator != null)
        {
            try
            {
                String code = autoCodeGenerator.genSerialCode("ROLL_CODE", null);
                if (code != null && !code.isEmpty()) return code;
            }
            catch (Exception ignored) { /* fall through to timestamp */ }
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int rand = (int) (Math.random() * 10000);
        return "RL" + ts + String.format("%04d", rand);
    }
}
