package com.cartisan.core.exception;

/**
 * 领域层异常。
 *
 * <p>用于表示领域模型中的业务规则违反。通常在以下场景抛出：</p>
 * <ul>
 *   <li>聚合根中业务规则校验失败（如余额不足、订单已关闭）</li>
 *   <li>实体中不变量被违反</li>
 *   <li>领域服务中业务逻辑失败</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class Order extends AbstractAggregateRoot {
 *     public void cancel() {
 *         if (status == OrderStatus.SHIPPED) {
 *             throw new DomainException(BaseCodeMessage.CONFLICT,
 *                 "Order cannot be cancelled after shipping");
 *         }
 *         this.status = OrderStatus.CANCELLED;
 *     }
 * }
 * }</pre>
 *
 * <h3>与基础设施异常的关系</h3>
 *
 * <p>当基础设施层抛出异常（如 {@code SQLException}）时，应在端口适配器中将其转换为
 * {@link DomainException} 或 {@link ApplicationException}，避免基础设施泄漏到领域层。</p>
 *
 * @see CartisanException
 * @see ApplicationException
 * @since 0.1.0
 */
public class DomainException extends CartisanException {

    /**
     * 构造器 - 无异常链。
     *
     * @param codeMessage 错误码信息
     * @param args 消息参数
     */
    public DomainException(CodeMessage codeMessage, Object... args) {
        super(codeMessage, args);
    }

    /**
     * 构造器 - 带异常链。
     *
     * <p>通常用于在端口适配器中转换基础设施异常。</p>
     *
     * @param codeMessage 错误码信息
     * @param cause 原始异常
     * @param args 消息参数
     */
    public DomainException(CodeMessage codeMessage, Throwable cause, Object... args) {
        super(codeMessage, cause, args);
    }
}
