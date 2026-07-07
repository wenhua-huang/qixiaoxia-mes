package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.ItemRecptReceiveBody;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;

public interface IWmItemRecptService
{
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity);
    public List<WmItemRecpt> selectWmItemRecptAll();
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId);
    public int insertWmItemRecpt(WmItemRecpt entity);
    public int updateWmItemRecpt(WmItemRecpt entity);
    public int deleteWmItemRecptByRecptId(Long recptId);
    public int deleteWmItemRecptByRecptIds(Long[] recptIds);

    /**
     * 确认收货（DRAFT → CONFIRMED）
     * 1. 更新库存
     * 2. 回写 PO 行的到货标记
     * 3. 更新 PO 状态（ORDERED → RECEIVING）
     */
    public void confirmItemRecpt(Long recptId);

    /**
     * 过账入库（CONFIRMED → POSTED）
     * 1. 回写 PO 行 quantityReceived
     * 2. 全收完 → PO status = RECEIVED
     */
    public void postItemRecpt(Long recptId);

    /**
     * 一键收货（移动端）：创建入库单头+行+确认收货，单个事务原子完成。
     *
     * 前端只需调这一个接口，无需分步调用 add + addLine + confirm。
     * 若任一步失败全部回滚，不会产生孤儿入库单头。
     */
    public void receiveWithLines(ItemRecptReceiveBody body);
}