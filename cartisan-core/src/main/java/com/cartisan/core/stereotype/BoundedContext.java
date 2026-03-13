package com.cartisan.core.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限界上下文注解。
 *
 * <p>标注在限界上下文顶层包的 {@code package-info.java} 上。
 * 子包自动属于同一上下文（由 F01-07 的 ArchUnit 规则向上查找实现）。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 文件：com/example/billing/package-info.java
 * {@literal @}BoundedContext(name = "Billing", subDomain = SubDomain.CORE)
 * package com.example.billing;
 * }</pre>
 *
 * <h2>设计约束</h2>
 * <ul>
 *   <li>一个包只能属于一个限界上下文（不支持 {@code @Repeatable}）</li>
 *   <li>应标注在上下文的顶层包，子包自动继承</li>
 *   <li>框架通过 ArchUnit 规则自动验证上下文边界</li>
 * </ul>
 *
 * @see SubDomain
 * @since 0.1.0
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BoundedContext {

    /**
     * 上下文名称。
     *
     * <p>名称应简洁明了，通常使用业务术语。
     * 例如："Billing"、"Ordering"、"Inventory"。</p>
     *
     * @return 上下文名称，非空
     */
    String name();

    /**
     * 子域类型。
     *
     * <p>标识该限界上下文在 DDD 战略设计中的类型。
     * 通过决策树确定：核心域 / 支撑域 / 通用域。</p>
     *
     * @return 子域类型
     */
    SubDomain subDomain();
}
