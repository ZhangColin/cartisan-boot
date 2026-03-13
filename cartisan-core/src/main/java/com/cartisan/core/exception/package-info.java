/**
 * Cartisan 异常体系。
 *
 * <h2>概览</h2>
 * <p>提供统一的异常处理机制，基于 DDD 分层架构设计，支持结构化错误码和参数化消息。</p>
 *
 * <h2>核心组件</h2>
 * <table border="1">
 *   <caption>异常体系核心组件</caption>
 *   <tr><th>组件</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>{@link com.cartisan.core.exception.CodeMessage}</td><td>接口</td><td>错误码契约（code、message、httpStatus）</td></tr>
 *   <tr><td>{@link com.cartisan.core.exception.BaseCodeMessage}</td><td>枚举</td><td>HTTP 规范错误码 + 通用业务错误码</td></tr>
 *   <tr><td>{@link com.cartisan.core.exception.CartisanException}</td><td>抽象类</td><td>异常基类，支持 MessageFormat 格式化</td></tr>
 *   <tr><td>{@link com.cartisan.core.exception.DomainException}</td><td>类</td><td>领域层异常（业务规则违反）</td></tr>
 *   <tr><td>{@link com.cartisan.core.exception.ApplicationException}</td><td>类</td><td>应用层异常（用例流程问题）</td></tr>
 * </table>
 *
 * <h2>使用指南</h2>
 *
 * <h3>1. 选择异常类型</h3>
 * <ul>
 *   <li>{@code DomainException} - 领域层业务规则违反（聚合根、实体、领域服务）</li>
 *   <li>{@code ApplicationException} - 应用层用例问题（应用服务、权限检查）</li>
 * </ul>
 *
 * <h3>2. 选择或定义错误码</h3>
 * <ul>
 *   <li>优先使用 {@link com.cartisan.core.exception.BaseCodeMessage}</li>
 *   <li>业务模块可定义自己的 {@code CodeMessage} 枚举</li>
 * </ul>
 *
 * <h3>3. 参数化消息</h3>
 * <p>使用 {@link java.text.MessageFormat} 占位符 {@code {0}}、{@code {1}} 等：</p>
 * <pre>{@code
 * // 定义带占位符的错误码
 * enum UserCodeMessage implements CodeMessage {
 *     USER_NOT_FOUND(404, "USER_NOT_FOUND", "User {0} not found");
 * }
 *
 * // 抛出异常时传入参数
 * throw new DomainException(UserCodeMessage.USER_NOT_FOUND, "john@example.com");
 * // 结果：getMessage() 返回 "User john@example.com not found"
 * }</pre>
 *
 * <h3>4. 保留异常链</h3>
 * <p>当转换底层异常时，保留原始异常以便调试：</p>
 * <pre>{@code
 * try {
 *     userRepository.findById(id);
 * } catch (SQLException e) {
 *     throw new ApplicationException(
 *         BaseCodeMessage.INTERNAL_SERVER_ERROR, e, "database");
 * }
 * }</pre>
 *
 * <h2>架构原则</h2>
 * <ul>
 *   <li>零外部依赖 - 仅使用 JDK 标准库</li>
 *   <li>错误语义自包含 - HTTP 状态码随错误码携带</li>
 *   <li>分层异常 - 领域层与应用层异常分离</li>
 *   <li>异常链保留 - 转换时保留原始异常</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.core.exception;
