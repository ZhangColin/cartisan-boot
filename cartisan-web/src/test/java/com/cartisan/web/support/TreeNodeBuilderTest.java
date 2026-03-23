package com.cartisan.web.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TreeNodeBuilder 单元测试")
class TreeNodeBuilderTest {

    @Test
    @DisplayName("给定扁平节点列表 - 调用 build - 返回树形结构")
    void shouldBuildTreeFromFlatList() {
        // Given
        List<TreeNode<Long>> nodes = List.of(
                new TreeNode<>(1L, "Node 1", 0L),
                new TreeNode<>(2L, "Node 2", 1L),
                new TreeNode<>(3L, "Node 3", 1L)
        );

        // When
        List<TreeNode<Long>> tree = TreeNodeBuilder.build(
                nodes,
                id -> String.valueOf(id),
                parentId -> String.valueOf(parentId),
                0L
        );

        // Then
        assertThat(tree).hasSize(1);

        TreeNode<Long> root = tree.get(0);
        assertThat(root.id()).isEqualTo(1L);
        assertThat(root.name()).isEqualTo("Node 1");
        assertThat(root.parentId()).isEqualTo(0L);
        assertThat(root.children()).hasSize(2);

        assertThat(root.children().get(0).id()).isEqualTo(2L);
        assertThat(root.children().get(0).name()).isEqualTo("Node 2");
        assertThat(root.children().get(0).parentId()).isEqualTo(1L);

        assertThat(root.children().get(1).id()).isEqualTo(3L);
        assertThat(root.children().get(1).name()).isEqualTo("Node 3");
        assertThat(root.children().get(1).parentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("给定多层嵌套节点 - 调用 build - 返回完整嵌套树形结构")
    void shouldHandleMultipleLevelNesting() {
        // Given
        List<TreeNode<Long>> nodes = List.of(
                new TreeNode<>(1L, "Root", 0L),
                new TreeNode<>(2L, "Child 1", 1L),
                new TreeNode<>(3L, "Child 2", 1L),
                new TreeNode<>(4L, "Grandchild 1", 2L),
                new TreeNode<>(5L, "Grandchild 2", 2L),
                new TreeNode<>(6L, "Great Grandchild", 4L)
        );

        // When
        List<TreeNode<Long>> tree = TreeNodeBuilder.build(
                nodes,
                id -> String.valueOf(id),
                parentId -> String.valueOf(parentId),
                0L
        );

        // Then
        assertThat(tree).hasSize(1);

        TreeNode<Long> root = tree.get(0);
        assertThat(root.id()).isEqualTo(1L);
        assertThat(root.children()).hasSize(2);

        TreeNode<Long> child1 = root.children().get(0);
        assertThat(child1.id()).isEqualTo(2L);
        assertThat(child1.children()).hasSize(2);

        TreeNode<Long> grandchild1 = child1.children().get(0);
        assertThat(grandchild1.id()).isEqualTo(4L);
        assertThat(grandchild1.children()).hasSize(1);

        TreeNode<Long> greatGrandchild = grandchild1.children().get(0);
        assertThat(greatGrandchild.id()).isEqualTo(6L);
        assertThat(greatGrandchild.children()).isEmpty();
    }

    @Test
    @DisplayName("给定空列表 - 调用 build - 返回空列表")
    void shouldHandleEmptyList() {
        // Given
        List<TreeNode<Long>> nodes = List.of();

        // When
        List<TreeNode<Long>> tree = TreeNodeBuilder.build(
                nodes,
                id -> String.valueOf(id),
                parentId -> String.valueOf(parentId),
                0L
        );

        // Then
        assertThat(tree).isEmpty();
    }

    @Test
    @DisplayName("给定孤儿节点 - 调用 build - 返回根节点并排除孤儿节点")
    void shouldHandleOrphanNodes() {
        // Given
        List<TreeNode<Long>> nodes = List.of(
                new TreeNode<>(1L, "Root", 0L),
                new TreeNode<>(2L, "Child", 1L),
                new TreeNode<>(3L, "Orphan", 999L)  // parentId=999 不存在
        );

        // When
        List<TreeNode<Long>> tree = TreeNodeBuilder.build(
                nodes,
                id -> String.valueOf(id),
                parentId -> String.valueOf(parentId),
                0L
        );

        // Then
        assertThat(tree).hasSize(1);

        TreeNode<Long> root = tree.get(0);
        assertThat(root.id()).isEqualTo(1L);
        assertThat(root.children()).hasSize(1);

        // Orphan node should not be in the tree
        assertThat(tree.stream()
                .flatMap(node -> flatten(node).stream())
                .noneMatch(node -> node.id().equals(3L)))
                .isTrue();
    }

    @Test
    @DisplayName("给定字符串类型 ID - 调用 build - 支持泛型类型")
    void shouldSupportGenericIdType() {
        // Given
        List<TreeNode<String>> nodes = List.of(
                new TreeNode<>("a", "Node A", "root"),
                new TreeNode<>("b", "Node B", "a"),
                new TreeNode<>("c", "Node C", "a")
        );

        // When
        List<TreeNode<String>> tree = TreeNodeBuilder.build(
                nodes,
                id -> id,
                parentId -> parentId,
                "root"
        );

        // Then
        assertThat(tree).hasSize(1);

        TreeNode<String> root = tree.get(0);
        assertThat(root.id()).isEqualTo("a");
        assertThat(root.name()).isEqualTo("Node A");
        assertThat(root.children()).hasSize(2);

        assertThat(root.children().get(0).id()).isEqualTo("b");
        assertThat(root.children().get(1).id()).isEqualTo("c");
    }

    private <T> List<TreeNode<T>> flatten(TreeNode<T> node) {
        List<TreeNode<T>> result = new java.util.ArrayList<>();
        result.add(node);
        for (TreeNode<T> child : node.children()) {
            result.addAll(flatten(child));
        }
        return result;
    }
}
