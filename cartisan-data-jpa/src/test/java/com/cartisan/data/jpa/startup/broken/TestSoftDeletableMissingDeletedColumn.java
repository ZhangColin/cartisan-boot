package com.cartisan.data.jpa.startup.broken;

import com.cartisan.core.domain.AggregateRoot;
import com.cartisan.data.jpa.domain.SoftDeletable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 故意违反 {@link SoftDeletable} 契约的测试实体：实现接口但<b>不</b>映射 {@code deleted} 列。
 *
 * <p>用于验证 {@code SoftDeletableRestrictionContributor} 的启动期 fail-fast——缺失
 * {@code deleted} 持久化列时，应用上下文应在<b>启动期</b>失败，而非运行期才抛 SQL 异常。</p>
 *
 * <p>本实体其它方面均合法（有 {@code @Id}），因此是<b>唯一</b>的失败触发点，避免与无关映射错误混淆。</p>
 */
@Entity(name = "test_sd_missing_deleted_column")
public class TestSoftDeletableMissingDeletedColumn
        implements AggregateRoot<TestSoftDeletableMissingDeletedColumn, Long>, SoftDeletable {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 故意空实现：不持久化删除标记（契约违反点）。
     */
    @Override
    public void markAsDeleted() {
        // 刻意无操作
    }

    /**
     * 故意返回 false：无持久化字段支撑。
     */
    @Override
    public boolean getDeleted() {
        return false;
    }
}
