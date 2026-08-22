package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import jakarta.annotation.PostConstruct;
import java.util.Set;

/**
 * 枚举扫描器，启动时扫描所有实现 BaseEnum 的枚举。
 *
 * <p>扫描根包由 {@code CartisanWebAutoConfiguration} 在装配时解析注入
 * （显式配置 {@code cartisan.web.enum-controller.scan-packages} 优先，
 * 缺省取应用主包，见 {@code ScanPackageResolver}），本类不再感知配置项。</p>
 *
 * @since 0.9.0
 */
public class EnumScanner {

    private final EnumRegistry registry;
    private final String[] scanPackages;

    public EnumScanner(EnumRegistry registry, String[] scanPackages) {
        this.registry = registry;
        this.scanPackages = scanPackages;
    }

    /**
     * 扫描指定包下的所有 BaseEnum 枚举并注册。
     *
     * @param basePackage 基础包名
     */
    public void scanBaseEnums(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEnum.class));

        Set<org.springframework.beans.factory.config.BeanDefinition> candidates =
            scanner.findCandidateComponents(basePackage);

        for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
            try {
                String className = candidate.getBeanClassName();
                Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());

                if (clazz.isEnum() && BaseEnum.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends BaseEnum<?>> enumClass = (Class<? extends BaseEnum<?>>) clazz;
                    registry.register(clazz.getSimpleName(), enumClass);
                }
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

    /**
     * 启动时扫描构造时注入的根包。
     */
    @PostConstruct
    public void autoScan() {
        for (String pkg : scanPackages) {
            scanBaseEnums(pkg);
        }
    }
}
