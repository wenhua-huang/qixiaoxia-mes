package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;
import com.ruoyi.system.domain.mes.wm.WmRtVendorLine;
import com.ruoyi.system.domain.mes.wm.tx.RtVendorTxBean;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmRtVendorMapper;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.wm.IWmRtVendorLineService;
import com.ruoyi.system.service.mes.wm.IWmRtVendorService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;

@Service
public class WmRtVendorServiceImpl implements IWmRtVendorService
{
    @Autowired
    private WmRtVendorMapper wmRtVendorMapper;

    @Autowired
    private IWmRtVendorLineService wmRtVendorLineService;

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @Autowired
    private PurOrderLineMapper purOrderLineMapper;

    @Override
    public List<WmRtVendor> selectWmRtVendorList(WmRtVendor entity) {
        return wmRtVendorMapper.selectWmRtVendorList(entity);
    }

    @Override
    public List<WmRtVendor> selectWmRtVendorAll() {
        return wmRtVendorMapper.selectWmRtVendorAll();
    }

    @Override
    public WmRtVendor selectWmRtVendorByRtId(Long rtId) {
        return wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
    }

    @Override
    @Transactional
    public int insertWmRtVendor(WmRtVendor entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmRtVendorMapper.insertWmRtVendor(entity);
    }

    @Override
    @Transactional
    public int updateWmRtVendor(WmRtVendor entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmRtVendorMapper.updateWmRtVendor(entity);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorByRtId(Long rtId) {
        return wmRtVendorMapper.deleteWmRtVendorByRtId(rtId);
    }

    @Override
    @Transactional
    public int deleteWmRtVendorByRtIds(Long[] rtIds) {
        return wmRtVendorMapper.deleteWmRtVendorByRtIds(rtIds);
    }

    /**
     * 确认退货单（DRAFT -> CONFIRMED）
     * 执行库存扣减（processRtVendor，内部 Redisson 锁+幂等）
     */
    @Override
    @Transactional
    public void confirmRtVendor(Long rtId) {
        WmRtVendor header = wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
        if (header == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new RuntimeException("仅草稿状态的退货单可确认，当前状态：" + header.getStatus());
        }
        WmRtVendorLine queryLine = new WmRtVendorLine();
        queryLine.setRtId(rtId);
        List<WmRtVendorLine> lines = wmRtVendorLineService.selectWmRtVendorLineList(queryLine);
        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("没有退货行，无法确认");
        }
        doConfirmRtVendor(header, lines);
    }

    /**
     * 确认核心逻辑（参数由调用者提供，避免重复 DB 查询）
     */
    private void doConfirmRtVendor(WmRtVendor header, List<WmRtVendorLine> lines) {
        // 1. 构建 TX beans
        List<RtVendorTxBean> txBeans = new ArrayList<>();
        for (WmRtVendorLine line : lines) {
            RtVendorTxBean bean = new RtVendorTxBean();
            bean.setSourceDocType("wm_rt_vendor");
            bean.setSourceDocId(header.getRtId());
            bean.setSourceDocCode(header.getRtCode());
            bean.setSourceDocLineId(line.getLineId());
            bean.setItemId(line.getItemId());
            bean.setItemCode(line.getItemCode());
            bean.setItemName(line.getItemName());
            bean.setSpecification(line.getSpecification());
            bean.setUnitOfMeasure(line.getUnitOfMeasure());
            bean.setUnitName(line.getUnitName());
            BigDecimal qty = line.getQuantityRt() != null ? line.getQuantityRt() : BigDecimal.ZERO;
            bean.setTransactionQuantity(qty);
            bean.setBatchId(line.getBatchId());
            bean.setBatchCode(line.getBatchCode());
            bean.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            bean.setWarehouseCode(line.getWarehouseCode() != null ? line.getWarehouseCode() : header.getWarehouseCode());
            bean.setWarehouseName(line.getWarehouseName() != null ? line.getWarehouseName() : header.getWarehouseName());
            bean.setLocationId(line.getLocationId());
            bean.setAreaId(line.getAreaId());
            bean.setVendorId(header.getVendorId());
            bean.setVendorCode(header.getVendorCode());
            bean.setVendorName(header.getVendorName());
            txBeans.add(bean);
        }
        // 2. 扣减库存（库存-1，内部 Redisson 锁+幂等检查）
        storageCoreService.processRtVendor(txBeans);
        // 3. 更新头状态 -> CONFIRMED
        header.setStatus("CONFIRMED");
        header.setConfirmTime(DateUtils.getNowDate());
        header.setConfirmBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmRtVendorMapper.updateWmRtVendor(header);
    }

    /**
     * 过账退货单（CONFIRMED -> POSTED）
     * 回写采购订单行已退货数量（quantity_returned 原子递增）
     */
    @Override
    @Transactional
    public void postRtVendor(Long rtId) {
        WmRtVendor header = wmRtVendorMapper.selectWmRtVendorByRtId(rtId);
        if (header == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (!"CONFIRMED".equals(header.getStatus())) {
            throw new RuntimeException("仅已确认的退货单可过账，当前状态：" + header.getStatus());
        }
        // 更新头 -> POSTED
        header.setStatus("POSTED");
        header.setPostTime(DateUtils.getNowDate());
        header.setPostBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmRtVendorMapper.updateWmRtVendor(header);
        // 回写采购订单行已退货数量
        writebackPoOnRtVendorPost(header);
    }

    /**
     * 回写采购订单：原子递增 quantity_returned
     * 通过 purOrderId + itemId 匹配 PO 行（V49 唯一约束保证同 PO 内物料唯一）
     */
    private void writebackPoOnRtVendorPost(WmRtVendor header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        // 加载 PO 行列表，构建 itemId -> PurOrderLine Map
        PurOrderLine query = new PurOrderLine();
        query.setOrderId(purOrderId);
        List<PurOrderLine> poLines = purOrderLineMapper.selectPurOrderLineList(query);
        if (poLines == null || poLines.isEmpty()) return;
        Map<Long, PurOrderLine> poLineByItemId = poLines.stream()
            .collect(Collectors.toMap(PurOrderLine::getItemId, Function.identity(), (a, b) -> a));

        // 加载退货行
        WmRtVendorLine queryLine = new WmRtVendorLine();
        queryLine.setRtId(header.getRtId());
        List<WmRtVendorLine> rtLines = wmRtVendorLineService.selectWmRtVendorLineList(queryLine);
        if (rtLines == null || rtLines.isEmpty()) return;

        // 逐行原子递增 quantity_returned
        for (WmRtVendorLine rtLine : rtLines) {
            PurOrderLine poLine = poLineByItemId.get(rtLine.getItemId());
            if (poLine != null) {
                BigDecimal qty = rtLine.getQuantityRt() != null ? rtLine.getQuantityRt() : BigDecimal.ZERO;
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    purOrderLineMapper.addQuantityReturned(poLine.getLineId(), qty);
                }
            }
        }
    }
}
