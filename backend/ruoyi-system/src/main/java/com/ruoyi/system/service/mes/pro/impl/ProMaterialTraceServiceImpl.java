package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
     * 深度追溯：后端一次性递归返回完整链路，替代前端 N+1 查询。
     * 同时构建三种视图数据：
     *   - tree：完整追溯树（递归展开所有分支），用于横向 DAG 流向图
     *   - chain：主链（深度优先第一条路径），兼容旧时间线视图
     *   - branches：主链之外的同节点兄弟跳，兼容旧分支展示
     * 用 visited 集合按 "type:id" 防环；循环节点标记 cycle=true 不再深入。
     */
    @Override
    public ProMaterialTraceChainResult traceChain(String startType, Long startId, String direction) {
        ProMaterialTraceChainResult result = new ProMaterialTraceChainResult(direction, startType, startId);
        Set<String> visited = new HashSet<>();
        Set<String> depthHit = new HashSet<>();  // 达到深度上限的节点
        boolean isForward = "forward".equals(direction);

        // 构建完整追溯树
        TraceTreeNode root = buildTraceTree(startType, startId, null, 0, visited, depthHit, isForward);
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
        } else if (!depthHit.isEmpty()) {
            result.setEndedReason("MAX_DEPTH");
        } else {
            result.setEndedReason("END");
        }
        return result;
    }

    /**
     * 递归构建追溯树。
     * @param nodeType 当前节点类型
     * @param nodeId   当前节点 ID
     * @param enterEdge 进入此节点的边（trace 记录），根节点为 null
     * @param depth    当前深度（根=0）
     * @param visited  全局已访问节点（按 "type:id"），防环
     * @param depthHit 达到深度上限的节点集合（用于 endedReason 判定）
     * @param isForward 正向/反向
     */
    private TraceTreeNode buildTraceTree(String nodeType, Long nodeId, ProMaterialTrace enterEdge,
                                         int depth, Set<String> visited, Set<String> depthHit,
                                         boolean isForward) {
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
            // 根节点描述稍后在父侧补充（取首条出边的 parent 描述）
            node.setNodeDesc(nodeType + " #" + nodeId);
        }

        String key = nodeType + ":" + nodeId;
        // 防环：已访问过的节点不再深入
        if (visited.contains(key)) {
            node.setCycle(true);
            return node;
        }
        // 深度限制
        if (depth >= MAX_TRACE_DEPTH) {
            depthHit.add(key);
            return node;
        }
        visited.add(key);

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
                TraceTreeNode child = buildTraceTree(childType, childId, hop, depth + 1, visited, depthHit, isForward);
                node.getChildren().add(child);
            }
        }
        return node;
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
