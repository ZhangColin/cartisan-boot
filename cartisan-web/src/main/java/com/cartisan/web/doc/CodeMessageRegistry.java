package com.cartisan.web.doc;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CodeMessage;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 错误码注册表：code → {@link CodeMessage} 常量索引。
 *
 * <p>{@link ErrorCodes} 注解只声明稳定公开的 code 标识（Java 注解成员不允许接口
 * 类型，无法直接引用 CodeMessage 常量），本注册表负责把 code 解析回错误码常量，
 * 保持错误码定义单点同源——注解引 code，注册表持有全部语义。</p>
 *
 * <p>{@link #scan} 预载框架通用错误码 {@link BaseCodeMessage}，再扫描指定包下
 * 实现 CodeMessage 的枚举。code 冲突（同 code 映射到不同常量）在注册时即失败，
 * 防止错误码语义二义。</p>
 *
 * <p>默认扫描包 {@code com.cartisan} + {@code com.example}（与 EnumScanner 同规），
 * 消费服务经 {@code cartisan.web.error-codes.scan-packages} 配置覆盖。</p>
 *
 * @since 0.2.0
 * @see ErrorCodes
 */
public final class CodeMessageRegistry {

    private static final String[] DEFAULT_SCAN_PACKAGES = {"com.cartisan", "com.example"};

    private final Map<String, CodeMessage> messages;
    private final String scanPackages;

    private CodeMessageRegistry(Map<String, CodeMessage> messages, String scanPackages) {
        this.messages = messages;
        this.scanPackages = scanPackages;
    }

    /**
     * 扫描构建注册表：预载 BaseCodeMessage + 逐包扫描 CodeMessage 枚举。
     *
     * @param basePackages 扫描根包（空数组长按默认包处理）
     * @return 不可变注册表
     */
    public static CodeMessageRegistry scan(String... basePackages) {
        Map<String, CodeMessage> index = new LinkedHashMap<>();
        Arrays.stream(BaseCodeMessage.values()).forEach(message -> register(index, message));

        String[] effectivePackages = basePackages.length > 0 ? basePackages : DEFAULT_SCAN_PACKAGES;
        for (String basePackage : effectivePackages) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(CodeMessage.class));
            scanner.findCandidateComponents(basePackage).forEach(candidate -> {
                String className = candidate.getBeanClassName();
                if (className == null) {
                    return;
                }
                registerEnum(index, className);
            });
        }
        return new CodeMessageRegistry(index, String.join(", ", effectivePackages));
    }

    /**
     * 由错误码常量直接构建（测试与显式装配用）。
     *
     * @param constants 错误码常量（通常为枚举的全部取值）
     * @return 不可变注册表
     */
    static CodeMessageRegistry of(CodeMessage... constants) {
        Map<String, CodeMessage> index = new LinkedHashMap<>();
        Arrays.stream(constants).forEach(message -> register(index, message));
        return new CodeMessageRegistry(index, String.join(", ", DEFAULT_SCAN_PACKAGES));
    }

    /**
     * 解析错误码，解析不到即失败。
     *
     * @param code 错误码标识
     * @return 对应的 CodeMessage 常量
     * @throws IllegalStateException code 未注册（错误码清单见扫描包内 CodeMessage 枚举）
     */
    public CodeMessage require(String code) {
        CodeMessage message = messages.get(code);
        if (message == null) {
            throw new IllegalStateException(
                    "错误码 %s 未注册：@ErrorCodes 引用的 code 必须存在于 CodeMessage 枚举"
                            .formatted(code) + hint());
        }
        return message;
    }

    /**
     * 宽松解析（可能为空）。
     *
     * @param code 错误码标识
     * @return 对应的 CodeMessage 常量
     */
    public Optional<CodeMessage> find(String code) {
        return Optional.ofNullable(messages.get(code));
    }

    /**
     * 注册单个枚举类：逐常量登记（枚举才有常量，普通实现类跳过）。
     */
    private static void registerEnum(Map<String, CodeMessage> index, String className) {
        try {
            Class<?> clazz = ClassUtils.forName(className, CodeMessageRegistry.class.getClassLoader());
            if (!clazz.isEnum() || !CodeMessage.class.isAssignableFrom(clazz)) {
                return;
            }
            Arrays.stream(clazz.getEnumConstants())
                    .map(CodeMessage.class::cast)
                    .forEach(message -> register(index, message));
        } catch (ClassNotFoundException | LinkageError e) {
            // 类不可达（并发卸载等边缘场景）：跳过，留 require 的失败兜底
        }
    }

    /**
     * 登记常量：同 code 同常量（如 BaseCodeMessage 预载后经扫描包再次发现）幂等；
     * 同 code 不同常量即失败。
     */
    private static void register(Map<String, CodeMessage> index, CodeMessage message) {
        CodeMessage existing = index.putIfAbsent(message.code(), message);
        if (existing != null && existing != message) {
            throw new IllegalStateException("错误码 %s 重复注册：%s 与 %s".formatted(
                    message.code(), describe(existing), describe(message)));
        }
    }

    private static String describe(CodeMessage message) {
        return "%s.%s".formatted(message.getClass().getName(), ((Enum<?>) message).name());
    }

    private String hint() {
        return "（扫描包 %s，可经 cartisan.web.error-codes.scan-packages 配置）".formatted(scanPackages);
    }
}
