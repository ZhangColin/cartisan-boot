/**
 * Fixture 工具包，提供测试数据构建能力。
 *
 * <h2>核心类</h2>
 * <ul>
 *   <li>{@link com.cartisan.test.fixture.FixtureStrings} — 字符串随机生成</li>
 *   <li>{@link com.cartisan.test.fixture.FixtureNumbers} — 数字随机生成</li>
 *   <li>{@link com.cartisan.test.fixture.FixtureDates} — 日期随机生成</li>
 *   <li>{@link com.cartisan.test.fixture.FixtureSeeds} — 全局种子管理</li>
 *   <li>{@link com.cartisan.test.fixture.FixtureBuilder} — 泛型对象构建器</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 随机数据生成
 * String name = FixtureStrings.randomString("USER_", 8);
 * long id = FixtureNumbers.randomId();
 * LocalDateTime yesterday = FixtureDates.pastDays(1);
 *
 * // 对象构建
 * Order order = FixtureBuilder.of(Order.class)
 *     .with("status", OrderStatus.CANCELLED)
 *     .build();
 *
 * // 种子可重复性
 * FixtureSeeds.setGlobalSeed(42);
 * String a1 = FixtureStrings.randomString();
 * FixtureSeeds.resetSeed();
 * FixtureSeeds.setGlobalSeed(42);
 * String b1 = FixtureStrings.randomString();
 * assertThat(b1).isEqualTo(a1);
 * }</pre>
 *
 * @since 0.1.0
 */
package com.cartisan.test.fixture;
