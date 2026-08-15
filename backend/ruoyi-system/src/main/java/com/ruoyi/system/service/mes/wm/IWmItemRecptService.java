package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.ItemRecptReceiveBody;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;

public interface IWmItemRecptService
{
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity);
    public List<WmItemRecpt> selectWmItemRecptAll();
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId);

    /**
     * 查询入库单详情（头 + 行）。供详情页一次请求取回，避免前端分两次调用。
     */
    public WmItemRecpt selectWmItemRecptDetail(Long recptId);
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
     * 一键收货（移动端）：创建入库单头+行+确认收货+过账，单个事务原子完成。
     *
     * 前端只需调这一个接口，无需分步调用 add + addLine + confirm。
     * 若任一步失败全部回滚，不会产生孤儿入库单头。
     *
     * @return 完成后的入库单详情（头 + 行，行含生成的批次码），供 App 展示
     */
    public WmItemRecpt receiveWithLines(ItemRecptReceiveBody body);

    /**
     * 从采购订单生成入库单草稿（不落库，返回前端编辑）。
     * 读取采购订单头+行，1:1 映射为入库单头+行，入库数量预填为「订购 − 已收」未收量。
     * 自动跳过已收完/已取消/已关闭的订单行；全部跳过则抛业务异常。
     */
    public WmItemRecpt buildFromPurOrder(Long orderId);
}