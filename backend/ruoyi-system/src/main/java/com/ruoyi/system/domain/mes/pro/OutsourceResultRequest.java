package com.ruoyi.system.domain.mes.pro;

import java.util.List;

/**
 * 外协厂商录分切结果请求 DTO（厂商登录后提交：每条子卷的物料/门幅/克重/长度/重量）
 *
 * <p>提交后子卷以 OUTSOURCED 状态落库（仍在厂商处），分切单 → SLITTING，待我方收货。
 *
 * @author qixiaoxia
 * @date 2026-08-02
 */
public class OutsourceResultRequest
{
    /** 子卷规格列表（至少1条，每条重量必须 > 0） */
    private List<SlittingRequest.ChildRollSpec> childRolls;

    public List<SlittingRequest.ChildRollSpec> getChildRolls() { return childRolls; }
    public void setChildRolls(List<SlittingRequest.ChildRollSpec> v) { this.childRolls = v; }
}
