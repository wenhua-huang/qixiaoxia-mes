package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.pro.ProMaterialTraceChainResult;
import com.ruoyi.system.domain.mes.pro.TraceTreeNode;
import com.ruoyi.system.service.mes.pro.IProMaterialTraceService;

/**
 * ProMaterialTraceService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProMaterialTraceServiceImpl implements IProMaterialTraceService
{
    @Autowired
    private ProMaterialTraceMapper proMaterialTraceMapper;

    @Override
    public ProMaterialTrace selectProMaterialTraceByTraceId(Long traceId) { return proMaterialTraceMapper.selectProMaterialTraceByTraceId(traceId); }

    @Override
    public List<ProMaterialTrace> selectProMaterialTraceList(ProMaterialTrace e) { return proMaterialTraceMapper.selectProMaterialTraceList(e); }

    @Override
    public List<ProMaterialTrace> selectAll() { return proMaterialTraceMapper.selectProMaterialTraceList(new ProMaterialTrace()); }

    @Override
    @Transactional
    public int insertProMaterialTrace(ProMaterialTrace e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return proMaterialTraceMapper.insertProMaterialTrace(e);
    }

    @Override
    public int updateProMaterialTrace(ProMaterialTrace e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proMaterialTraceMapper.updateProMaterialTrace(e);
    }

    @Override
    public int deleteProMaterialTraceByTraceIds(Long[] traceIds) { return proMaterialTraceMapper.deleteProMaterialTraceByTraceIds(traceIds); }

    @Override
    public int deleteProMaterialTraceByTraceId(Long traceId) { return proMaterialTraceMapper.deleteProMaterialTraceByTraceId(traceId); }

    @Override
    public List<ProMaterialTrace> traceForward(String parentType, Long parentId) {
        return proMaterialTraceMapper.selectByParent(parentType, parentId);
    }

    @Override
    public List<ProMaterialTrace> traceBackward(String childType, Long childId) {
        return proMaterialTraceMapper.selectByChild(childType, childId);
    }

    /** 最大追溯深度，防止异常数据导致无限递归 */
    private static final int MAX_TRACE_DEPTH = 20;

    /**
     * 业务单据节点类型：作为追溯分支的「视角锚点」。
     * 菱形汇聚时，引用节点的「首现于 XXX」文案取最近一个锚点节点的描述。
     */
    private static final Set<String> BRANCH_ANCHOR_TYPES = Set.of(
            "CARD", "OUTSOURCE_ORDER", "WORKORDER", "SALES_OUT", "PUR_ORDER", "VENDOR");

    /**
     * 深度追溯：后端一次性递归返回完整链路，替代前端 N+1 查询。
     * 同时构建三种视图数据：
     *   - tree：完整追溯树（递归展开所有分支），用于横向 DAG 流向图
     *   - chain：主链（深度优先第一条路径），兼容旧时间线视图
     *   - branches：主链之外的同节点兄弟跳，兼容旧分支展示
     *
     * 防环/汇聚采用两层集合：
     *   - ancestors：当前 DFS 路径（进入 add、回溯 remove），命中即真环 cycle=true
     *   - expanded：全遍历只增不减，命中即菱形汇聚 reference=true，指向首现分支
     */
    @Override
    public ProMaterialTraceChainResult traceChain(String startType, Long startId, String direction) {
        ProMaterialTraceChainResult result = new ProMaterialTraceChainResult(direction, startType, startId);
        Set<String> ancestors = new HashSet<>();
        Set<String> expanded = new HashSet<>();
        Map<String, String> firstBranchOf = new HashMap<>();
        Map<String, Integer> refCount = new HashMap<>();
        Set<String> depthHit = new HashSet<>();
        Set<String> cycleHit = new HashSet<>();
        boolean isForward = "forward".equals(direction);

        // 构建完整追溯树（根节点不归属任何分支）
        TraceTreeNode root = buildTraceTree(startType, startId, null, 0,
                ancestors, expanded, firstBranchOf, refCount,
                depthHit, cycleHit, null, isForward);
        // 把被引用次数回填到首次出现的节点上（角标「🔗N 处关联」）
        annotateRefCount(root, refCount);
        result.setTree(root);

        // 兼容旧字段：从树提取主链 + 兄弟分支
        List<ProMaterialTrace> chain = new ArrayList<>();
        List<ProMaterialTrace> branches = new ArrayList<>();
        flattenTreeToLegacy(root, chain, branches, new HashSet<>());
        result.setChain(chain);
        result.setBranches(branches);

        // 终止原因判定
        if (root.getChildren().isEmpty() && root.getTraceType() == null) {
            result.setEndedReason("NOT_FOUND");
        } else if (!cycleHit.isEmpty()) {
            result.setEndedReason("LOOP");
        } else if (!depthHit.isEmpty()) {
            result.setEndedReason("MAX_DEPTH");
        } else {
            result.setEndedReason("END");
        }
        return result;
    }

    /**
     * 递归构建追溯树。
     *
     * @param ancestors      当前 DFS 路径上的节点 key（进入 add、回溯 remove），命中表示真环
     * @param expanded       全遍历已展开节点 key（只增不减），命中表示菱形汇聚
     * @param firstBranchOf  key → 首次出现所在分支锚点描述
     * @param refCount       key → 被其他分支引用的次数
     * @param depthHit       达到深度上限的节点集合
     * @param cycleHit       命中真环的节点集合
     * @param currentBranch  当前路径最近一个业务单据锚点描述（用于「首现于 XXX」）
     */
    private TraceTreeNode buildTraceTree(String nodeType, Long nodeId, ProMaterialTrace enterEdge,
                                         int depth,
                                         Set<String> ancestors, Set<String> expanded,
                                         Map<String, String> firstBranchOf, Map<String, Integer> refCount,
                                         Set<String> depthHit, Set<String> cycleHit,
                                         String currentBranch, boolean isForward) {
        TraceTreeNode node = new TraceTreeNode();
        node.setNodeType(nodeType);
        node.setNodeId(nodeId);
        node.setDepth(depth);

        // 从入边提取展示信息
        if (enterEdge != null) {
            node.setNodeDesc(isForward ? enterEdge.getChildDesc() : enterEdge.getParentDesc());
            node.setTraceType(enterEdge.getTraceType());
            node.setQuantity(enterEdge.getQuantity());
            node.setUnitName(enterEdge.getUnitName());
            node.setItemName(enterEdge.getItemName());
            node.setBatchCode(enterEdge.getBatchCode());
        } else {
            node.setNodeDesc(nodeType + " #" + nodeId);
        }

        String key = nodeType + ":" + nodeId;
        // ① 真环：当前路径回到祖先（数据异常，标红截断）
        if (ancestors.contains(key)) {
            node.setCycle(true);
            cycleHit.add(key);
            return node;
        }
        // 深度限制
        if (depth >= MAX_TRACE_DEPTH) {
            depthHit.add(key);
            return node;
        }
        // ② 菱形汇聚：该节点已在其他分支展开过，渲染为引用卡，不再递归
        if (expanded.contains(key)) {
            node.setReference(true);
            node.setRefToBranch(firstBranchOf.get(key));
            refCount.merge(key, 1, Integer::sum);
            return node;
        }

        ancestors.add(key);
        expanded.add(key);
        // 记录首次出现时所属的业务分支（仅在有锚点时记录，避免 null 覆盖）
        if (currentBranch != null) {
            firstBranchOf.putIfAbsent(key, currentBranch);
        }

        // 业务单据节点本身作为新的分支锚点
        String nextBranch = BRANCH_ANCHOR_TYPES.contains(nodeType) ? node.getNodeDesc() : currentBranch;

        // 查所有子跳
        List<ProMaterialTrace> hops = isForward
            ? proMaterialTraceMapper.selectByParent(nodeType, nodeId)
            : proMaterialTraceMapper.selectByChild(nodeType, nodeId);

        if (hops != null) {
            for (ProMaterialTrace hop : hops) {
                // 根节点（enterEdge==null）补全描述：取第一条出边的源描述
                if (enterEdge == null && node.getChildren().isEmpty()) {
                    node.setNodeDesc(isForward ? hop.getParentDesc() : hop.getChildDesc());
                    if (hop.getItemName() != null) node.setItemName(hop.getItemName());
                }
                String childType = isForward ? hop.getChildType() : hop.getParentType();
                Long childId = isForward ? hop.getChildId() : hop.getParentId();
                if (childType == null || childId == null) continue;
                TraceTreeNode child = buildTraceTree(childType, childId, hop, depth + 1,
                        ancestors, expanded, firstBranchOf, refCount,
                        depthHit, cycleHit, nextBranch, isForward);
                node.getChildren().add(child);
            }
        }
        // 回溯：只退出当前路径，expanded 保留（跨分支去重）
        ancestors.remove(key);
        return node;
    }

    /** 后序遍历，把 refCount 中累计的被引用次数回填到首次出现（非 reference/cycle）的节点上 */
    private void annotateRefCount(TraceTreeNode node, Map<String, Integer> refCount) {
        if (node == null) return;
        if (!node.isReference() && !node.isCycle()) {
            String key = node.getNodeType() + ":" + node.getNodeId();
            Integer n = refCount.get(key);
            if (n != null && n > 0) node.setRefCount(n);
        }
        if (node.getChildren() != null) {
            for (TraceTreeNode c : node.getChildren()) annotateRefCount(c, refCount);
        }
    }

    /**
     * 从追溯树提取主链（深度优先第一条路径）+ 兄弟分支，兼容旧前端字段。
     * 主链节点转回 ProMaterialTrace 入 chain；同节点其他子跳转回 branches。
     * 注意：树节点已不含完整 trace 信息（只有摘要），这里仅用 nodeType/nodeId 标识，
     * 旧前端依赖的 trace 字段会缺失，故旧视图建议升级为基于 tree 的渲染。
     */
    private void flattenTreeToLegacy(TraceTreeNode node, List<ProMaterialTrace> chain,
                                     List<ProMaterialTrace> branches, Set<String> mainPathVisited) {
        if (node == null) return;
        String key = node.getNodeType() + ":" + node.getNodeId();
        // 仅根节点不作为"跳"加入 chain；子节点转成轻量 trace 加入主链
        if (node.getTraceType() != null && mainPathVisited.add(key)) {
            ProMaterialTrace t = new ProMaterialTrace();
            t.setTraceType(node.getTraceType());
            t.setQuantity(node.getQuantity());
            t.setUnitName(node.getUnitName());
            t.setItemName(node.getItemName());
            t.setBatchCode(node.getBatchCode());
            t.setParentDesc(node.getNodeDesc());
            t.setChildDesc(node.getNodeDesc());
            chain.add(t);
        }
        // 主链只跟第一个子节点，其余子节点作为兄弟分支
        List<TraceTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (i == 0) {
                flattenTreeToLegacy(children.get(i), chain, branches, mainPathVisited);
            } else {
                TraceTreeNode sib = children.get(i);
                if (sib.getTraceType() != null) {
                    ProMaterialTrace t = new ProMaterialTrace();
                    t.setTraceType(sib.getTraceType());
                    t.setQuantity(sib.getQuantity());
                    t.setUnitName(sib.getUnitName());
                    t.setItemName(sib.getItemName());
                    t.setBatchCode(sib.getBatchCode());
                    t.setChildDesc(sib.getNodeDesc());
                    branches.add(t);
                }
            }
        }
    }
}
