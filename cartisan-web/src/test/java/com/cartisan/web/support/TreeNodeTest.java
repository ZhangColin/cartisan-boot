package com.cartisan.web.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TreeNode 单元测试")
class TreeNodeTest {

    @Test
    @DisplayName("给定基本构造参数 - 创建 TreeNode - 返回正确属性")
    void shouldCreateTreeNodeWithBasicConstructor() {
        // Given
        Long id = 1L;
        String name = "Test Node";
        Long parentId = 0L;

        // When
        TreeNode<Long> node = new TreeNode<>(id, name, parentId);

        // Then
        assertThat(node.id()).isEqualTo(id);
        assertThat(node.name()).isEqualTo(name);
        assertThat(node.parentId()).isEqualTo(parentId);
        assertThat(node.children()).isEmpty();
    }

    @Test
    @DisplayName("给定带子节点的构造参数 - 创建 TreeNode - 返回包含子节点的树")
    void shouldCreateTreeNodeWithChildren() {
        // Given
        TreeNode<Long> child1 = new TreeNode<>(2L, "Child 1", 1L);
        TreeNode<Long> child2 = new TreeNode<>(3L, "Child 2", 1L);
        List<TreeNode<Long>> children = List.of(child1, child2);

        // When
        TreeNode<Long> parent = new TreeNode<>(1L, "Parent", 0L, children);

        // Then
        assertThat(parent.id()).isEqualTo(1L);
        assertThat(parent.name()).isEqualTo("Parent");
        assertThat(parent.parentId()).isEqualTo(0L);
        assertThat(parent.children()).hasSize(2);
        assertThat(parent.children()).isEqualTo(children);
    }

    @Test
    @DisplayName("给定 setChildren 方法 - 设置子节点 - 子节点被正确设置")
    void shouldSetChildren() {
        // Given
        TreeNode<Long> node = new TreeNode<>(1L, "Node", 0L);
        List<TreeNode<Long>> children = List.of(
            new TreeNode<>(2L, "Child", 1L)
        );

        // When
        node.setChildren(children);

        // Then
        assertThat(node.children()).isEqualTo(children);
    }

    @Test
    @DisplayName("给定空子节点列表 - 设置 children - 支持空列表")
    void shouldSupportEmptyChildren() {
        // Given
        List<TreeNode<Long>> emptyChildren = List.of();
        TreeNode<Long> node = new TreeNode<>(1L, "Node", 0L, emptyChildren);

        // When & Then
        assertThat(node.children()).isEmpty();
    }
}
