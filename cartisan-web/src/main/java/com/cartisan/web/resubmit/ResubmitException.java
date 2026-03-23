package com.cartisan.web.resubmit;

/**
 * 重复提交异常。
 *
 * <p>当检测到重复提交时抛出此异常，通常由 {@link ResubmitAspect} 在拦截
 * 带 {@link PreventResubmit} 注解的方法时抛出。</p>
 *
 * <p>这是一个运行时异常，会被全局异常处理器捕获并返回适当的 HTTP 响应。</p>
 *
 * @see ResubmitAspect
 * @see PreventResubmit
 */
public class ResubmitException extends RuntimeException {

    /**
     * 构造重复提交异常。
     *
     * @param message 异常消息
     */
    public ResubmitException(String message) {
        super(message);
    }

    /**
     * 构造重复提交异常（带默认消息）。
     */
    public ResubmitException() {
        super("请勿重复提交");
    }
}
