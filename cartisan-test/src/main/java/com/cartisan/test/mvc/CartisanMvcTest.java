package com.cartisan.test.mvc;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * cartisan MVC 切片测试组合注解。
 *
 * <p>{@code @WebMvcTest} 默认不加载 cartisan-web 的 Jackson 枚举配置与
 * {@code BaseEnumConverter}，导致切片内枚举参数绑定 / 序列化行为与生产不一致。
 * 本注解在 {@code @WebMvcTest} 之上自动带入 {@link CartisanMvcTestConfiguration}，
 * 对齐生产行为：</p>
 * <ul>
 *   <li>枚举 {@code @RequestParam}/{@code @PathVariable} 按 Integer code 绑定</li>
 *   <li>响应 JSON 中 BaseEnum 字段序列化为 Integer code</li>
 *   <li>全局异常处理生效（非法 code → 400 信封，含取值域表）</li>
 * </ul>
 *
 * <p>controller 过滤语义与 {@code @WebMvcTest} 一致：指定 {@code controllers}
 * 时只装配指定 controller（过滤作用于 {@code @SpringBootApplication} 主类的
 * 组件扫描）。可与 {@code @WithRequestContext} 叠加使用。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @CartisanMvcTest(controllers = OrderController.class)
 * class OrderControllerTest {
 *
 *     @Autowired
 *     private MockMvc mvc;
 * }
 * }</pre>
 *
 * <p>切片上下文需要能向上找到 {@code @SpringBootConfiguration}（消费服务的
 * 主应用类即可）；嵌套 {@code @Configuration} + {@code @ComponentScan} 的
 * 写法虽能启动，但 controller 过滤不作用于该路径，不推荐。</p>
 *
 * @since 0.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@WebMvcTest
@Import(CartisanMvcTestConfiguration.class)
public @interface CartisanMvcTest {

    /**
     * {@code controllers} 的别名，指定切片装配的 controller。
     *
     * @return controller 类数组
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};

    /**
     * 指定切片装配的 controller；缺省时装配扫描到的全部 controller
     * （与 {@code @WebMvcTest} 语义一致）。
     *
     * @return controller 类数组
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};

    /**
     * 追加测试环境属性（形如 {@code key=value}），优先级高于配置文件。
     *
     * @return 属性数组
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "properties")
    String[] properties() default {};

    /**
     * 切片内排除的自动配置类。
     *
     * @return 自动配置类数组
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "excludeAutoConfiguration")
    Class<?>[] excludeAutoConfiguration() default {};
}
