package com.cartisan.core.exception;

/**
 * 应用层异常。
 *
 * <p>用于表示应用服务层和用例层面的问题。通常在以下场景抛出：</p>
 * <ul>
 *   <li>应用服务中参数校验失败</li>
 *   <li>权限检查失败</li>
 *   <li>用例前置条件不满足</li>
 *   <li>基础设施异常在端口适配器中转换后抛出</li>
 * </ul>
 *
 * <h2>与 DomainException 的区别</h2>
 *
 * <table border="1">
 *   <caption>异常类型对比</caption>
 *   <tr><th>异常类型</th><th>使用场景</th><th>示例</th></tr>
 *   <tr><td>DomainException</td><td>业务规则违反</td><td>余额不足、订单已关闭</td></tr>
 *   <tr><td>ApplicationException</td><td>用例流程问题</td><td>用户未登录、参数格式错误</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Service
 * public class UserService {
 *     public void updateUser(Long userId, UpdateUserCommand command) {
 *         if (userId == null) {
 *             throw new ApplicationException(BaseCodeMessage.INVALID_PARAMETER, "userId");
 *         }
 *         if (!securityContext.isAuthenticated()) {
 *             throw new ApplicationException(BaseCodeMessage.UNAUTHORIZED);
 *         }
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @see CartisanException
 * @see DomainException
 * @since 0.1.0
 */
public class ApplicationException extends CartisanException {

    /**
     * 构造器 - 无异常链。
     *
     * @param codeMessage 错误码信息
     * @param args 消息参数
     */
    public ApplicationException(CodeMessage codeMessage, Object... args) {
        super(codeMessage, args);
    }

    /**
     * 构造器 - 带异常链。
     *
     * <p>通常用于在端口适配器中转换基础设施异常或封装下层异常。</p>
     *
     * @param codeMessage 错误码信息
     * @param cause 原始异常
     * @param args 消息参数
     */
    public ApplicationException(CodeMessage codeMessage, Throwable cause, Object... args) {
        super(codeMessage, cause, args);
    }
}
