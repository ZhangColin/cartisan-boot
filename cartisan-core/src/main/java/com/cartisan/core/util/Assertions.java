package com.cartisan.core.util;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CodeMessage;
import com.cartisan.core.exception.DomainException;

import java.util.Optional;

/**
 * Design by Contract 断言工具。
 *
 * <p>提供前置条件、后置条件和存在性断言，简化防御式编程。</p>
 *
 * <h3>方法概览</h3>
 * <ul>
 *   <li>{@code require(condition, codeMessage)} — 前置条件断言，失败抛出 {@code DomainException}</li>
 *   <li>{@code ensure(condition, message)} — 后置条件断言，失败抛出 {@code IllegalStateException}</li>
 *   <li>{@code requirePresent(optional)} — 存在性断言（快捷版），使用标准 404</li>
 *   <li>{@code requirePresent(optional, codeMessage)} — 存在性断言（完整版），自定义错误码</li>
 * </ul>
 *
 * <h3>异常类型语义</h3>
 * <ul>
 *   <li>{@code require} 失败 → {@code DomainException}：调用者责任 = 业务规则违反 = 4xx</li>
 *   <li>{@code ensure} 失败 → {@code IllegalStateException}：实现者责任 = 代码 bug = 500</li>
 *   <li>{@code requirePresent} 失败 → {@code DomainException}：资源不存在 = 404 或其他 4xx</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class Assertions {

    /**
     * 私有构造函数，防止实例化。
     */
    private Assertions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== 前置条件断言 ==========

    /**
     * 校验前置条件，不满足时抛出 {@link DomainException}。
     *
     * <p>用于检查方法调用的前置条件是否满足。如果条件为 {@code false}，
     * 抛出携带指定错误码的 {@code DomainException}。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * public void cancel() {
     *     Assertions.require(status != OrderStatus.SHIPPED, OrderError.CANNOT_CANCEL_SHIPPED);
     *     this.status = OrderStatus.CANCELLED;
     * }
     * }</pre>
     *
     * @param condition 前置条件，为 {@code true} 时静默通过
     * @param codeMessage 错误码信息，条件为 {@code false} 时用于创建异常
     * @throws DomainException 当 {@code condition} 为 {@code false} 时
     */
    public static void require(boolean condition, CodeMessage codeMessage, Object... args) {
        if (!condition) {
            throw new DomainException(codeMessage, args);
        }
    }

    // ========== 后置条件断言 ==========

    /**
     * 校验后置条件，不满足时抛出 {@link IllegalStateException}。
     *
     * <p>用于检查方法执行后的后置条件是否满足。如果条件为 {@code false}，
     * 抛出 {@code IllegalStateException}，表示代码存在 bug。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * public void addItem(OrderItem item) {
     *     require(item != null, OrderError.ITEM_REQUIRED);
     *     this.items.add(item);
     *     ensure(this.items.contains(item), "item should be present after add");
     * }
     * }</pre>
     *
     * @param condition 后置条件，为 {@code true} 时静默通过
     * @param message 失败原因描述
     * @throws IllegalStateException 当 {@code condition} 为 {@code false} 时
     */
    public static void ensure(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Postcondition violated: " + message);
        }
    }

    // ========== 存在性断言 ==========

    /**
     * 要求 Optional 有值，无值时抛出 {@link DomainException}（NOT_FOUND）。
     *
     * <p>快捷版本，使用标准 404 错误码。适用于不需要区分资源类型的场景。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * public OrderDto getOrder(Long id) {
     *     Order order = Assertions.requirePresent(orderRepository.findById(id));
     *     return OrderDto.from(order);
     * }
     * }</pre>
     *
     * @param optional Optional 实例
     * @param <T> 值类型
     * @return Optional 包含的值（保证非 null）
     * @throws DomainException 当 {@code optional} 为空时，错误码为 {@code NOT_FOUND}
     */
    public static <T> T requirePresent(Optional<T> optional) {
        return requirePresent(optional, BaseCodeMessage.NOT_FOUND);
    }

    /**
     * 要求 Optional 有值，无值时抛出 {@link DomainException}（指定错误码）。
     *
     * <p>完整版本，支持自定义错误码。适用于需要区分资源类型的场景。</p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * public OrderDto getUserOrder(Long userId, Long orderId) {
     *     User user = Assertions.requirePresent(
     *         userRepository.findById(userId),
     *         UserError.USER_NOT_FOUND
     *     );
     *     Order order = Assertions.requirePresent(
     *         orderRepository.findById(orderId),
     *         OrderError.ORDER_NOT_FOUND
     *     );
     *     return OrderDto.from(order);
     * }
     * }</pre>
     *
     * @param optional Optional 实例
     * @param codeMessage 错误码信息
     * @param <T> 值类型
     * @return Optional 包含的值（保证非 null）
     * @throws DomainException 当 {@code optional} 为空时，携带指定的 {@code codeMessage}
     */
    public static <T> T requirePresent(Optional<T> optional, CodeMessage codeMessage) {
        if (optional.isEmpty()) {
            throw new DomainException(codeMessage);
        }
        return optional.get();
    }
}
