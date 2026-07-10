package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProWorkorderDetailVO;
import com.ruoyi.system.domain.mes.pro.ProWorkorderParam;

/**
 * 生产工单Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProWorkorderService
{
    /**
     * 查询生产工单
     *
     * @param workorderId 生产工单主键
     * @return 生产工单
     */
    public ProWorkorder selectProWorkorderByWorkorderId(Long workorderId);

    /**
     * 查询生产工单列表
     *
     * @param proWorkorder 生产工单
     * @return 生产工单集合
     */
    public List<ProWorkorder> selectProWorkorderList(ProWorkorder proWorkorder);

    /**
     * 查询所有生产工单
     *
     * @return 生产工单集合
     */
    public List<ProWorkorder> selectAll();

    /**
     * 检查工单编码唯一性
     *
     * @param proWorkorder 生产工单
     * @return 结果
     */
    public boolean checkWorkorderCodeUnique(ProWorkorder proWorkorder);

    /**
     * 新增生产工单
     *
     * @param proWorkorder 生产工单
     * @return 结果
     */
    public int insertProWorkorder(ProWorkorder proWorkorder);

    /**
     * 修改生产工单
     *
     * @param proWorkorder 生产工单
     * @return 结果
     */
    public int updateProWorkorder(ProWorkorder proWorkorder);

    /**
     * 批量删除生产工单
     *
     * @param workorderIds 需要删除的生产工单主键集合
     * @return 结果
     */
    public int deleteProWorkorderByWorkorderIds(Long[] workorderIds);

    /**
     * 删除生产工单信息
     *
     * @param workorderId 生产工单主键
     * @return 结果
     */
    public int deleteProWorkorderByWorkorderId(Long workorderId);

    /**
     * 开工：PREPARE → PRODUCING
     *
     * @param workorderId 生产工单主键
     * @return 结果
     */
    public int startProduction(Long workorderId);

    /**
     * 取消工单：PREPARE/PRODUCING → CANCEL
     *
     * @param workorderId 生产工单主键
     * @return 结果
     */
    public int cancelWorkorder(Long workorderId);

    /**
     * 物料齐套检查（实时计算，不持久化）
     * 读取工单 BOM → 实时查询 wm 库存 → 返回每行物料的供需情况
     *
     * @param workorderId 生产工单主键
     * @return 齐套检查结果列表
     */
    public List<java.util.Map<String, Object>> checkMaterialReadiness(Long workorderId);

    /**
     * 开工前检查 + 自动生成领料单 + 开工（事务性）
     * 四步：物料齐套检查 → 排产检查 → 按工序生成领料单 → 开工
     * 任一步骤失败则事务回滚，返回每步检查结果
     *
     * @param workorderId 生产工单主键
     * @param forceSchedule 排产检查FAIL时是否豁免开工（true=跳过排产强制开工，需配overrideReason）
     * @param overrideReason 豁免理由（forceSchedule=true且排产FAIL时必填，记入工单变更记录）
     * @return 每步检查结果列表 [{step, stepName, status:PASS/FAIL/SKIP/OVERRIDE, message, details}]
     */
    public List<java.util.Map<String, Object>> preStartCheck(Long workorderId, boolean forceSchedule, String overrideReason);

    /**
     * 创建生产工单（含BOM和工序参数）
     *
     * @param workorder 生产工单
     * @param bomList 工单BOM组成列表
     * @param paramList 工单工序参数值列表
     * @return 创建后的生产工单（含workorderId）
     */
    public ProWorkorder createWorkorderWithBom(ProWorkorder workorder, List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList);

    /**
     * 查询生产工单详情（含BOM和工序参数）
     *
     * @param workorderId 生产工单主键
     * @return 生产工单详情VO
     */
    public ProWorkorderDetailVO getWorkorderDetail(Long workorderId);

    /**
     * 修改生产工单（含BOM和工序参数）
     * 在一个事务中完成：工单头更新 → 旧BOM删除 + 新BOM插入 → 工序参数upsert
     *
     * @param workorder 生产工单
     * @param bomList 工单BOM组成列表（全量替换）
     * @param paramList 工单工序参数值列表（按templateId upsert）
     * @return 修改后的生产工单
     */
    public ProWorkorder updateWorkorderWithBom(ProWorkorder workorder, List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList);

    /**
     * 偏离检测：比较提交的 BOM/参数 与 路线标准，返回偏离结果。
     * 纯计算，不持久化。前端据此弹窗询问用户是否创建变体。
     *
     * @param workorder 生产工单（含 productId、routeProductId）
     * @param bomList 提交的工单BOM列表
     * @param paramList 提交的工单工序参数列表
     * @return 偏离检测结果
     */
    public com.ruoyi.system.domain.mes.pro.ProWorkorderDeviationVO checkDeviation(
            ProWorkorder workorder, List<ProWorkorderBom> bomList, List<ProWorkorderParam> paramList);
}
