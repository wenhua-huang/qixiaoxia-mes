package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.ruoyi.common.enums.WmProductSalesConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesBoxMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesLineMapper;
import com.ruoyi.system.service.mes.wm.IWmProductSalesBoxService;

/**
 * 销售出库-装箱明细业务层
 * - 新增时自动算体积（长×宽×高 / 1_000_000 cm³→m³）
 * - 箱号为空时自动 BOX-NNN（同一出库单内递增）
 * - 仅 PACKED 可删；已发运的箱不可删（需先删除对应发运单）
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
@Service
public class WmProductSalesBoxServiceImpl implements IWmProductSalesBoxService
{
    @Autowired private WmProductSalesBoxMapper wmProductSalesBoxMapper;
    @Autowired private WmProductSalesLineMapper wmProductSalesLineMapper;
    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(10);
    }

    @Override
    public List<WmProductSalesBox> selectWmProductSalesBoxList(WmProductSalesBox entity) {
        return wmProductSalesBoxMapper.selectWmProductSalesBoxList(entity);
    }

    @Override
    public WmProductSalesBox selectWmProductSalesBoxByBoxId(Long boxId) {
        return wmProductSalesBoxMapper.selectWmProductSalesBoxByBoxId(boxId);
    }

    @Override
    public List<WmProductSalesBox> selectBoxesBySalesId(Long salesId) {
        return wmProductSalesBoxMapper.selectBoxesBySalesId(salesId);
    }

    @Override
    public List<WmProductSalesBox> selectBoxesByShipmentId(Long shipmentId) {
        return wmProductSalesBoxMapper.selectBoxesByShipmentId(shipmentId);
    }

    @Override
    public int insertWmProductSalesBox(WmProductSalesBox entity) {
        if (entity.getSalesId() == null) throw new ServiceException("出库单ID不能为空");
        // 同一出库单并发装箱需串行，否则 BOX-NNN 会重号；锁与发运共用同一把，保证发运勾箱时装箱已提交
        return lockTemplate.executeWithResult("wm:salesout:lock:" + entity.getSalesId(), 10,
                () -> txTemplate.execute(status -> doInsert(entity)));
    }

    private int doInsert(WmProductSalesBox entity) {
        if (entity.getBoxNo() == null || entity.getBoxNo().isEmpty()) {
            entity.setBoxNo(genNextBoxNo(entity.getSalesId()));
        }
        if (entity.getStatus() == null || entity.getStatus().isEmpty()) {
            entity.setStatus(WmProductSalesConstants.BOX_STATUS_PACKED);
        }
        // 装箱量不得超过该行已出库确认量（出库确认扣库存后才能装箱发运）
        validatePackedAgainstPosted(entity, null);
        entity.setVolume(calcVolume(entity));
        entity.setCreateTime(DateUtils.getNowDate());
        entity.setCreateBy(SecurityUtils.getUsername());
        return wmProductSalesBoxMapper.insertWmProductSalesBox(entity);
    }

    @Override
    public int updateWmProductSalesBox(WmProductSalesBox entity) {
        WmProductSalesBox exist = wmProductSalesBoxMapper.selectWmProductSalesBoxByBoxId(entity.getBoxId());
        if (exist == null) throw new ServiceException("装箱记录不存在");
        if (WmProductSalesConstants.BOX_STATUS_SHIPPED.equals(exist.getStatus())) {
            throw new ServiceException("已发运的箱不可修改（需删除对应发运单）");
        }
        Long salesId = exist.getSalesId();
        // 与新增/发运共用同一把锁，避免装箱并发改数量绕过 postedQuantity 校验
        return lockTemplate.executeWithResult("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doUpdate(entity, exist)));
    }

    private int doUpdate(WmProductSalesBox entity, WmProductSalesBox exist) {
        // 以库中 salesId/lineId 为准做装箱量校验（前端不可篡改归属）
        entity.setSalesId(exist.getSalesId());
        if (entity.getLineId() == null) entity.setLineId(exist.getLineId());
        validatePackedAgainstPosted(entity, entity.getBoxId());
        entity.setVolume(calcVolume(entity));
        entity.setUpdateTime(DateUtils.getNowDate());
        entity.setUpdateBy(SecurityUtils.getUsername());
        return wmProductSalesBoxMapper.updateWmProductSalesBox(entity);
    }

    @Override
    public int deleteWmProductSalesBoxByBoxId(Long boxId) {
        WmProductSalesBox exist = wmProductSalesBoxMapper.selectWmProductSalesBoxByBoxId(boxId);
        if (exist == null) return 0;
        if (WmProductSalesConstants.BOX_STATUS_SHIPPED.equals(exist.getStatus())) {
            throw new ServiceException("已发运的箱不可删除（需先删除对应发运单）");
        }
        return wmProductSalesBoxMapper.deleteWmProductSalesBoxByBoxId(boxId);
    }

    @Override
    public int deleteWmProductSalesBoxByBoxIds(Long[] boxIds) {
        for (Long id : boxIds) {
            WmProductSalesBox exist = wmProductSalesBoxMapper.selectWmProductSalesBoxByBoxId(id);
            if (exist != null && WmProductSalesConstants.BOX_STATUS_SHIPPED.equals(exist.getStatus())) {
                throw new ServiceException("箱[" + exist.getBoxNo() + "]已发运，不可删除");
            }
        }
        return wmProductSalesBoxMapper.deleteWmProductSalesBoxByBoxIds(boxIds);
    }

    // ════════════════════ 内部工具 ════════════════════

    /**
     * 装箱量校验：该出库行累计装箱量（PACKED + SHIPPED，编辑时排除自身）不得超过已出库确认量。
     * DRAFT 时 quantityPosted=0，自然无法装箱。
     */
    private void validatePackedAgainstPosted(WmProductSalesBox entity, Long excludeBoxId) {
        Long lineId = entity.getLineId();
        if (lineId == null) return; // 兼容未关联行的历史数据
        WmProductSalesLine line = wmProductSalesLineMapper.selectWmProductSalesLineByLineId(lineId);
        if (line == null) throw new ServiceException("出库行[" + lineId + "]不存在");
        BigDecimal posted = line.getQuantityPosted() != null ? line.getQuantityPosted() : BigDecimal.ZERO;

        WmProductSalesBox q = new WmProductSalesBox();
        q.setSalesId(entity.getSalesId());
        q.setLineId(lineId);
        BigDecimal alreadyPacked = BigDecimal.ZERO;
        for (WmProductSalesBox b : wmProductSalesBoxMapper.selectWmProductSalesBoxList(q)) {
            if (excludeBoxId != null && excludeBoxId.equals(b.getBoxId())) continue;
            if (b.getQuantity() != null) alreadyPacked = alreadyPacked.add(b.getQuantity());
        }
        BigDecimal thisQty = entity.getQuantity() != null ? entity.getQuantity() : BigDecimal.ZERO;
        if (alreadyPacked.add(thisQty).compareTo(posted) > 0) {
            throw new ServiceException("物料[" + line.getItemCode() + "]装箱量(" + alreadyPacked.add(thisQty)
                    + ")超过已出库确认量(" + posted + ")，请先完成出库确认");
        }
    }

    /** 生成下一箱号 BOX-NNN（同 sales 内递增，从已有最大序号 +1） */
    private String genNextBoxNo(Long salesId) {
        Integer maxSeq = wmProductSalesBoxMapper.selectMaxBoxSeqBySalesId(salesId);
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return String.format("BOX-%03d", next);
    }

    /** 体积 = 长 × 宽 × 高（cm³ → m³，除以 1_000_000），任一维度为空返回 0 */
    private static BigDecimal calcVolume(WmProductSalesBox b) {
        BigDecimal l = nz(b.getBoxLength());
        BigDecimal w = nz(b.getBoxWidth());
        BigDecimal h = nz(b.getBoxHeight());
        if (l.signum() == 0 || w.signum() == 0 || h.signum() == 0) return BigDecimal.ZERO;
        return l.multiply(w).multiply(h)
                .divide(new BigDecimal("1000000"), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
