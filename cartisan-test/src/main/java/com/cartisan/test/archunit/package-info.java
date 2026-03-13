/**
 * Cartisan ArchUnit 规则集
 *
 * <p>本包提供可复用的 DDD 架构规则，业务项目继承后自动守护分层架构：</p>
 * <ul>
 *   <li>{@link com.cartisan.test.archunit.CartisanLayeringRules} — DDD 分层依赖方向验证</li>
 *   <li>{@link com.cartisan.test.archunit.CartisanNamingRules} — 命名规范验证</li>
 *   <li>{@link com.cartisan.test.archunit.CartisanProhibitionRules} — 禁止反模式</li>
 *   <li>{@link com.cartisan.test.archunit.CartisanArchRules} — 聚合全部规则的入口</li>
 * </ul>
 *
 * <p><strong>业务项目使用方式：</strong></p>
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.yourcompany")
 * public class ArchitectureTest extends CartisanArchRules {
 *     // 完了。所有规则自动生效。
 * }
 * }</pre>
 *
 * @package com.cartisan.test.archunit
 * @since 0.1.0
 */
package com.cartisan.test.archunit;
