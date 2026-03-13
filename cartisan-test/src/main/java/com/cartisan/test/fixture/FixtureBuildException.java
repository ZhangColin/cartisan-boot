package com.cartisan.test.fixture;

/**
 * Fixture 对象构建失败时抛出的异常。
 *
 * <p>继承 {@link RuntimeException}，为非受检异常，测试代码中无需强制捕获。</p>
 *
 * <h3>典型触发场景</h3>
 * <ul>
 *   <li>目标类没有无参构造函数</li>
 *   <li>目标类是 Record 类型（一期不支持）</li>
 *   <li>指定的字段不存在</li>
 *   <li>字段赋值失败（setter 抛出异常）</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class FixtureBuildException extends RuntimeException {

    /**
     * 构造仅包含消息的异常。
     *
     * @param message 异常消息
     */
    public FixtureBuildException(String message) {
        super(message);
    }

    /**
     * 构造包含消息和原因的异常。
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public FixtureBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
