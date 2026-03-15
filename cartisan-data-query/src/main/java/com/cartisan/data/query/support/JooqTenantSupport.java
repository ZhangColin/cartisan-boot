package com.cartisan.data.query.support;

import com.cartisan.security.context.TenantContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.TableField;

/**
 * jOOQ 多租户查询支持工具类。
 *
 * <p>提供租户过滤条件的便捷生成方法，支持按表字段添加租户等值条件。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * import static com.cartisan.data.query.support.JooqTenantSupport.eqTenantId;
 * import static com.example.db.Tables.USER;
 *
 * // 在 WHERE 子句中使用
 * List<UserRecord> users = dslContext.selectFrom(USER)
 *     .where(USER.NAME.like("%name%"))
 *     .and(eqTenantId(USER.TENANT_ID))  // 自动添加租户过滤
 *     .fetch();
 * }</pre>
 *
 * <h3>行为说明</h3>
 * <ul>
 *   <li>有租户上下文时：返回 {@code tenantIdField.eq(tenantId)} 等值条件</li>
 *   <li>无租户上下文时：返回 {@link DSL#noCondition()}，不影响查询</li>
 * </ul>
 *
 * <h3>依赖说明</h3>
 * <p>本类依赖 {@code cartisan-security} 的 {@link TenantContext}。
 * 使用 {@code compileOnly} 依赖范围，运行时由使用者引入 {@code cartisan-security}。</p>
 *
 * @since 0.3.0
 */
public final class JooqTenantSupport {

    /**
     * 防止实例化。
     *
     * @throws UnsupportedOperationException 始终抛出，表示工具类不可实例化
     */
    private JooqTenantSupport() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 为指定表的租户字段生成等值过滤条件。
     *
     * <p>根据当前租户上下文生成条件：</p>
     * <ul>
     *   <li>有租户：返回 {@code tenantIdField.eq(tenantId)}</li>
     *   <li>无租户：返回 {@link DSL#noCondition()}（空条件）</li>
     * </ul>
     *
     * @param tenantIdField 表的租户 ID 字段（如 {@code USER.TENANT_ID}）
     * @return jOOQ Condition 对象，永远非 null
     * @throws NullPointerException 若 {@code tenantIdField} 为 null
     */
    public static Condition eqTenantId(TableField<?, Long> tenantIdField) {
        // 前置检查
        if (tenantIdField == null) {
            throw new NullPointerException("tenantIdField cannot be null");
        }

        // 获取当前租户 ID
        Long tenantId = TenantContext.getCurrentTenantId();

        // 根据租户上下文返回条件
        if (tenantId != null) {
            return tenantIdField.eq(tenantId);
        } else {
            return DSL.noCondition();
        }
    }
}
