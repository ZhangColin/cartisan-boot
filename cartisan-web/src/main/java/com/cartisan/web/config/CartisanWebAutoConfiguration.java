package com.cartisan.web.config;

import com.cartisan.web.context.RequestContextFilter;
import com.cartisan.web.controller.EnumController;
import com.cartisan.web.doc.CodeMessageRegistry;
import com.cartisan.web.doc.ErrorCodeOperationCustomizer;
import com.cartisan.web.doc.ErrorCodesValidator;
import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.enums.EnumScanner;
import com.cartisan.web.exception.GlobalExceptionHandler;
import com.cartisan.web.filter.RequestLogFilter;
import com.cartisan.web.resubmit.PreventResubmit;
import com.cartisan.web.resubmit.ResubmitAspect;
import com.cartisan.web.resubmit.ResubmitLock;
import com.cartisan.web.support.ScanPackageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * cartisan-web 模块的 Spring Boot 自动配置。
 *
 * <p>注册 Web 层核心组件：
 * <ul>
 *   <li>{@link RequestContextFilter} — 请求上下文初始化</li>
 *   <li>{@link RequestLogFilter} — 请求日志记录</li>
 *   <li>{@link GlobalExceptionHandler} — 全局异常处理</li>
 *   <li>{@link ResubmitAspect} — 防重复提交切面（当 Redis 可用时）</li>
 * </ul>
 *
 * <p><strong>引入即用</strong>：添加 cartisan-web 依赖后，无需 {@code @ComponentScan}，
 * 这些组件会自动注册。</p>
 *
 * <h3>条件装配</h3>
 * <p>仅在 Web 应用环境中生效（非 Web 应用如批处理不需要这些组件）。</p>
 *
 * <h3>用户覆盖</h3>
 * <p>核心组件强制注册，不使用 {@code @ConditionalOnMissingBean}。
 * 用户需要替换时，通过排除 AutoConfiguration 或显式注册自定义 Bean 处理。</p>
 *
 * @since 0.2.0
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, WebMvcAutoConfiguration.class})
@ConditionalOnWebApplication
@org.springframework.context.annotation.Import(JacksonConfiguration.class)
@EnableConfigurationProperties(EnumErrorProperties.class)
public class CartisanWebAutoConfiguration implements WebMvcConfigurer {

    /**
     * 注册请求上下文 Filter。
     *
     * <p>Bean 名称使用 {@code cartisanRequestContextFilter}，与之前 {@code @Component} 注解时的名称一致，
     * 保持向后兼容。</p>
     *
     * @return RequestContextFilter 实例
     */
    @Bean("cartisanRequestContextFilter")
    public RequestContextFilter requestContextFilter() {
        return new RequestContextFilter();
    }

    /**
     * 注册请求日志记录 Filter。
     *
     * <p>依赖 {@link RequestContextFilter}，需要在请求上下文初始化之后执行。
     * 设置 Order 值为 {@code Ordered.HIGHEST_PRECEDENCE + 1}，确保在 RequestContextFilter 之后。</p>
     *
     * @return RequestLogFilter 实例
     */
    @Bean
    public RequestLogFilter requestLogFilter() {
        return new RequestLogFilter();
    }

    /**
     * 注册全局异常处理器。
     *
     * <p>{@link GlobalExceptionHandler} 类本身保留 {@code @ControllerAdvice} 注解，
     * 这是 Spring MVC 识别异常处理器的必要注解。</p>
     *
     * <p>业务码覆盖配置（{@link EnumErrorProperties}）注入后，
     * BaseEnum 取值错误可映射为消费服务的业务码，默认 400。</p>
     *
     * @param enumErrorProperties 枚举错误业务码配置
     * @return GlobalExceptionHandler 实例
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(EnumErrorProperties enumErrorProperties) {
        return new GlobalExceptionHandler(enumErrorProperties);
    }

    /**
     * 注册防重复提交锁。
     *
     * <p>仅在 Redis 可用时注册。</p>
     *
     * @param redisTemplate Redis 模板
     * @return ResubmitLock 实例
     */
    @Bean
    @ConditionalOnClass(PreventResubmit.class)
    @ConditionalOnMissingBean
    public ResubmitLock resubmitLock(StringRedisTemplate redisTemplate) {
        return new ResubmitLock(redisTemplate);
    }

    /**
     * 注册防重复提交切面。
     *
     * <p>仅在 ResubmitLock 可用时注册。</p>
     *
     * @param resubmitLock 防重复提交锁
     * @param objectMapper JSON 序列化工具
     * @return ResubmitAspect 实例
     */
    @Bean
    @ConditionalOnClass(PreventResubmit.class)
    @ConditionalOnMissingBean
    public ResubmitAspect resubmitAspect(ResubmitLock resubmitLock, ObjectMapper objectMapper) {
        return new ResubmitAspect(resubmitLock, objectMapper);
    }

    /**
     * 注册 BaseEnum Converter Factory。
     *
     * 支持 @RequestParam、@PathVariable 直接使用 BaseEnum 类型参数。
     *
     * @see com.cartisan.web.config.BaseEnumConverter
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new BaseEnumConverter());
    }

    /**
     * 注册枚举注册表。
     *
     * <p>作为 Spring Bean 管理，确保自动配置时不依赖 @ComponentScan。</p>
     *
     * @return EnumRegistry 实例
     */
    @Bean
    public EnumRegistry enumRegistry() {
        return new EnumRegistry();
    }

    /**
     * 注册枚举扫描器。
     *
     * <p>扫描根包经 {@link ScanPackageResolver} 解析：显式配置
     * {@code cartisan.web.enum-controller.scan-packages} 优先，缺省取应用主包
     * （{@code @SpringBootApplication} 所在包），任意包名的服务零配置生效。</p>
     *
     * @param enumRegistry 枚举注册表
     * @param beanFactory  BeanFactory（查询 AutoConfigurationPackages）
     * @param scanPackages 显式配置的扫描包
     * @return EnumScanner 实例
     */
    @Bean
    public EnumScanner enumScanner(EnumRegistry enumRegistry, BeanFactory beanFactory,
            @Value("${cartisan.web.enum-controller.scan-packages:}") String[] scanPackages) {
        return new EnumScanner(enumRegistry, ScanPackageResolver.resolve(scanPackages, beanFactory));
    }

    /**
     * 注册枚举 Controller（默认实现）。
     *
     * <p>可通过配置项 {@code cartisan.web.enum-controller.enabled} 禁用。
     *
     * @param enumRegistry 枚举注册表
     * @return EnumController 实例
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "cartisan.web.enum-controller",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public EnumController enumController(EnumRegistry enumRegistry) {
        return new EnumController(enumRegistry);
    }

    /**
     * BaseEnum 展示名虚拟属性的 Jackson 集成（opt-in）。
     *
     * <p>{@code cartisan.web.enum-name-fields.enabled=true} 时注册
     * {@link BaseEnumNameSerializerModifier}：序列化输出为 BaseEnum 属性自动追加
     * {@code xxxName} 展示名字段。默认关闭——该行为改变所有响应 JSON 的形状，
     * 由消费方显式开启；springdoc 侧的 schema 同步（SpringDocIntegrationConfiguration
     * 内）共用同一开关，保证两跳一致。</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "cartisan.web.enum-name-fields", name = "enabled", havingValue = "true")
    static class EnumNameFieldsJacksonConfiguration {

        /**
         * 注册追加 {@code xxxName} 的 Jackson 模块。
         *
         * @return Jackson2ObjectMapperBuilderCustomizer 配置器
         */
        @Bean
        public Jackson2ObjectMapperBuilderCustomizer baseEnumNameFieldJacksonCustomizer() {
            // 显式命名：裸 SimpleModule 的 typeId 是类名，会与 JacksonConfiguration 的
            // BaseEnum 模块撞 id 被 registerModule 去重丢弃；
            // Consumer 变体追加：Spring 6.2 起 modules(...)/modulesToInstall(...)
            // 是替换语义，直接调用会覆盖其他 customizer 的模块
            SimpleModule module = new SimpleModule("CartisanBaseEnumNameFields");
            module.setSerializerModifier(new BaseEnumNameSerializerModifier());
            return builder -> builder.modules(modules -> modules.add(module));
        }
    }

    /**
     * springdoc 集成（classpath 存在 springdoc 时生效）。
     *
     * <p>注册 {@link BaseEnumModelConverter}，使 {@code /v3/api-docs} 中 BaseEnum 字段
     * 渲染为 {@code type=integer} + code→名称对照，对齐运行时 Jackson 契约；
     * 注册 {@link com.cartisan.web.doc.ErrorCodeOperationCustomizer} 与配套的
     * {@link com.cartisan.web.doc.CodeMessageRegistry}，把 {@code @ErrorCodes}
     * 声明的错误码渲染进端点描述。未引入 springdoc 的服务不加载本配置类，零影响。</p>
     *
     * <p>springdoc 的 ModelConverterRegistrar 收集容器内全部
     * {@code io.swagger.v3.core.converter.ModelConverter} Bean，自动接入全局解析链；
     * OperationCustomizer Bean 同样由 springdoc 自动收集，对所有分组生效。</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SpringDocConfiguration.class)
    static class SpringDocIntegrationConfiguration {

        /**
         * 注册 BaseEnum ModelConverter。
         *
         * @return BaseEnumModelConverter 实例
         */
        @Bean
        public BaseEnumModelConverter baseEnumModelConverter() {
            return new BaseEnumModelConverter();
        }

        /**
         * 注册 BaseEnum 展示名的 schema 同步 converter（opt-in）。
         *
         * <p>与 Jackson 侧的 {@link BaseEnumNameSerializerModifier} 共用
         * {@code cartisan.web.enum-name-fields.enabled} 开关：schema 中为 BaseEnum
         * 属性同步合成 {@code xxxName} string 属性，两跳不漂移。</p>
         *
         * @param objectMapper 应用 ObjectMapper（内省属性名，兼容命名策略）
         * @return BaseEnumNameFieldModelConverter 实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "cartisan.web.enum-name-fields", name = "enabled", havingValue = "true")
        public BaseEnumNameFieldModelConverter baseEnumNameFieldModelConverter(ObjectMapper objectMapper) {
            return new BaseEnumNameFieldModelConverter(objectMapper);
        }

        /**
         * 构建错误码注册表（BaseCodeMessage 预载 + 扫描 CodeMessage 枚举）。
         *
         * <p>扫描包经 {@code cartisan.web.error-codes.scan-packages} 配置，
         * 缺省取应用主包（与 EnumScanner 同规，经 {@link ScanPackageResolver} 解析）。</p>
         *
         * @param beanFactory  BeanFactory（查询 AutoConfigurationPackages）
         * @param scanPackages 显式配置的扫描包
         * @return CodeMessageRegistry 实例
         */
        @Bean
        public CodeMessageRegistry codeMessageRegistry(BeanFactory beanFactory,
                @Value("${cartisan.web.error-codes.scan-packages:}") String[] scanPackages) {
            return CodeMessageRegistry.scan(ScanPackageResolver.resolve(scanPackages, beanFactory));
        }

        /**
         * 注册错误码渲染 Customizer。
         *
         * @param registry 错误码注册表
         * @return ErrorCodeOperationCustomizer 实例
         */
        @Bean
        public ErrorCodeOperationCustomizer errorCodeOperationCustomizer(CodeMessageRegistry registry) {
            return new ErrorCodeOperationCustomizer(registry);
        }

        /**
         * 注册 @ErrorCodes 启动校验器（启动即报出不可解析的 code，防 typo）。
         *
         * <p>仅在 Spring MVC 环境（存在 RequestMappingHandlerMapping）注册；
         * 外层 auto-config 已 after WebMvcAutoConfiguration，条件判定时序正确。</p>
         *
         * @param handlerMapping MVC handler 映射
         * @param registry 错误码注册表
         * @return ErrorCodesValidator 实例
         */
        @Bean
        @ConditionalOnBean(RequestMappingHandlerMapping.class)
        public ErrorCodesValidator errorCodesValidator(
                @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                CodeMessageRegistry registry) {
            return new ErrorCodesValidator(handlerMapping, registry);
        }
    }
}
