package com.ruoyi.system.domain.mes.pro;

import java.util.ArrayList;
import java.util.List;

/**
 * 物料追溯链路查询结果
 *
 * <p>由 {@code traceChain} 深度追溯接口返回，包含完整链路与终止原因。
 *
 * <h3>endedReason 取值</h3>
 * <ul>
 *   <li><b>END</b>        —— 正常追溯到链路末端（最后一跳的下游无记录）</li>
 *   <li><b>NOT_FOUND</b>  —— 起始节点本身就没有任何追溯记录（第一跳就空）</li>
 *   <li><b>LOOP</b>       —— 检测到循环引用，主动停止防死循环</li>
 *   <li><b>MAX_DEPTH</b>  —— 达到最大深度（20 跳），链路过长被截断</li>
 * </ul>
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public class ProMaterialTraceChainResult
{
    /** 追溯方向：forward 正向（追去向）/ backward 反向（追来源） */
    private String direction;

    /** 起始节点类型 */
    private String startType;

    /** 起始节点 ID */
    private Long startId;

    /** 链路（按追溯顺序排列的 trace 记录，每条代表一跳） */
    private List<ProMaterialTrace> chain = new ArrayList<>();

    /**
     * 兄弟分支：主链路上某些节点存在多个去向/来源时，未被主链采信的其他跳。
     * 例如一个批次被发到多个工单——主链只跟第一个工单，其余工单的 ISSUE 记录在此列出。
     * 扁平列表，不递归展开（工单通常是 trace 末端，避免树形爆炸）。
     */
    private List<ProMaterialTrace> branches = new ArrayList<>();

    /**
     * 完整追溯树（根节点）。递归展开所有分支，用于横向 DAG 可视化。
     * 前端优先使用 tree 渲染流向图；chain/branches 保留兼容旧客户端。
     */
    private TraceTreeNode tree;

    /** 终止原因：END / NOT_FOUND / LOOP / MAX_DEPTH */
    private String endedReason;

    public ProMaterialTraceChainResult() {}

    public ProMaterialTraceChainResult(String direction, String startType, Long startId) {
        this.direction = direction;
        this.startType = startType;
        this.startId = startId;
    }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getStartType() { return startType; }
    public void setStartType(String startType) { this.startType = startType; }

    public Long getStartId() { return startId; }
    public void setStartId(Long startId) { this.startId = startId; }

    public List<ProMaterialTrace> getChain() { return chain; }
    public void setChain(List<ProMaterialTrace> chain) { this.chain = chain; }

    public List<ProMaterialTrace> getBranches() { return branches; }
    public void setBranches(List<ProMaterialTrace> branches) { this.branches = branches; }

    public TraceTreeNode getTree() { return tree; }
    public void setTree(TraceTreeNode tree) { this.tree = tree; }

    public String getEndedReason() { return endedReason; }
    public void setEndedReason(String endedReason) { this.endedReason = endedReason; }
}
