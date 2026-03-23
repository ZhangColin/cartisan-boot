package com.cartisan.web.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 树节点泛型类。
 *
 * <p>用于前端树组件数据，支持不同类型的 ID。</p>
 *
 * @param <T> ID 类型（Long、String 等）
 */
public class TreeNode<T> {

    private T id;
    private String name;
    private T parentId;
    private List<TreeNode<T>> children;

    /**
     * 默认构造函数。
     */
    public TreeNode() {
        this.children = new ArrayList<>();
    }

    /**
     * 全参构造函数。
     *
     * @param id       节点 ID
     * @param name     节点名称
     * @param parentId 父节点 ID
     */
    public TreeNode(T id, String name, T parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.children = new ArrayList<>();
    }

    /**
     * 全含子节点构造函数。
     *
     * @param id       节点 ID
     * @param name     节点名称
     * @param parentId 父节点 ID
     * @param children 子节点列表
     */
    public TreeNode(T id, String name, T parentId, List<TreeNode<T>> children) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.children = children != null ? children : new ArrayList<>();
    }

    public T id() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T parentId() {
        return parentId;
    }

    public void setParentId(T parentId) {
        this.parentId = parentId;
    }

    public List<TreeNode<T>> children() {
        return children;
    }

    public void setChildren(List<TreeNode<T>> children) {
        this.children = children != null ? children : new ArrayList<>();
    }
}
