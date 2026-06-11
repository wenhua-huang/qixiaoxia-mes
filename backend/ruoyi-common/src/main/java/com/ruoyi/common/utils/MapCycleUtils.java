package com.ruoyi.common.utils;

import java.util.*;

/**
 * BOM循环引用检测工具（DFS + visited/recStack）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MapCycleUtils
{
    /**
     * 检测在已有边集合中加入 (parentId -> childId) 是否会产生环
     *
     * @param parentId 父节点ID
     * @param childId  子节点ID
     * @param edges    已有边集合，每条边为 [parentId, childId]
     * @return true = 会产生环
     */
    public static boolean hasCycle(Long parentId, Long childId, List<Long[]> edges)
    {
        Map<Long, Set<Long>> graph = new HashMap<>();
        if (edges != null)
        {
            for (Long[] edge : edges)
            {
                graph.computeIfAbsent(edge[0], k -> new HashSet<>()).add(edge[1]);
            }
        }
        // 加入待检测的边
        graph.computeIfAbsent(parentId, k -> new HashSet<>()).add(childId);

        Set<Long> visited = new HashSet<>();
        Set<Long> recStack = new HashSet<>();

        // dfs_entry: 从 parentId 出发检测
        if (hasCycleDfs(parentId, graph, visited, recStack))
        {
            return true;
        }
        return false;
    }

    private static boolean hasCycleDfs(Long node, Map<Long, Set<Long>> graph,
                                        Set<Long> visited, Set<Long> recStack)
    {
        if (recStack.contains(node))
        {
            return true;
        }
        if (visited.contains(node))
        {
            return false;
        }
        visited.add(node);
        recStack.add(node);
        Set<Long> children = graph.getOrDefault(node, Collections.emptySet());
        for (Long child : children)
        {
            if (hasCycleDfs(child, graph, visited, recStack))
            {
                return true;
            }
        }
        recStack.remove(node);
        return false;
    }
}
