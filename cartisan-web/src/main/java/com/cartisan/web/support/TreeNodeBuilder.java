package com.cartisan.web.support;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 树节点构建器。
 *
 * <p>将扁平节点列表转换为树形结构。</p>
 */
public final class TreeNodeBuilder {

    private TreeNodeBuilder() {
        // Utility class, prevent instantiation
    }

    /**
     * 将扁平节点列表构建为树形结构。
     *
     * @param nodes           扁平节点列表
     * @param idMapper        ID 映射函数（将节点 ID 转换为字符串用于分组）
     * @param parentIdMapper  父 ID 映射函数（将父节点 ID 转换为字符串用于分组）
     * @param rootParentId    根节点的父 ID 值
     * @param <T>             ID 类型
     * @return 树形结构的根节点列表
     */
    public static <T> List<TreeNode<T>> build(
            List<TreeNode<T>> nodes,
            Function<T, String> idMapper,
            Function<T, String> parentIdMapper,
            T rootParentId) {

        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        // 按 parentId 分组
        Map<String, List<TreeNode<T>>> grouped = nodes.stream()
                .collect(Collectors.groupingBy(
                        node -> parentIdMapper.apply(node.parentId())
                ));

        // 设置 children
        nodes.forEach(node -> {
            String nodeId = idMapper.apply(node.id());
            node.setChildren(grouped.getOrDefault(nodeId, List.of()));
        });

        // 返回根节点
        String rootParentKey = parentIdMapper.apply(rootParentId);
        return grouped.getOrDefault(rootParentKey, List.of());
    }
}
