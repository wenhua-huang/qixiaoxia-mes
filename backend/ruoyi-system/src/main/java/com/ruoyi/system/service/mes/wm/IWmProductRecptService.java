package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;

public interface IWmProductRecptService
{
    public List<WmProductRecpt> selectWmProductRecptList(WmProductRecpt entity);
    public List<WmProductRecpt> selectWmProductRecptAll();
    public WmProductRecpt selectWmProductRecptByRecptId(Long recptId);

    /**
     * 查询入库单详情（头 + 行）。供详情页一次请求取回，避免前端分两次调用。
     */
    public WmProductRecpt selectWmProductRecptDetail(Long recptId);
    public int insertWmProductRecpt(WmProductRecpt entity);
    public int updateWmProductRecpt(WmProductRecpt entity);
    public int deleteWmProductRecptByRecptId(Long recptId);
    public int deleteWmProductRecptByRecptIds(Long[] recptIds);

    /**
     * 确认收货（DRAFT -> CONFIRMED）- 更新库存。
     * 库存变更内部通过 Redisson 锁 + TransactionTemplate 保证并发安全。
     */
    public void confirmProductRecpt(Long recptId);

    /**
     * 过账入库（CONFIRMED -> POSTED）。
     * 库存数据已在 confirmProductRecpt 阶段更新，此处仅做状态变更。
     */
    public void postProductRecpt(Long recptId);
}
