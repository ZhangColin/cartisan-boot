package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import jakarta.annotation.PostConstruct;
import java.util.Set;

/**
 * 枚举扫描器，启动时扫描所有实现 BaseEnum 的枚举。
 *
 * @since 0.9.0
 */
@Component
public class EnumScanner {

    private final EnumRegistry registry;

    @Value("${cartisan.web.enum-controller.scan-packages:}")
    private String[] scanPackages;

    public EnumScanner(EnumRegistry registry) {
        this.registry = registry;
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
     * 启动时扫描，默认扫描 com.cartisan 和 com.example 包。
     */
    @PostConstruct
    public void autoScan() {
        String[] packagesToScan = scanPackages.length > 0 ? scanPackages : new String[]{"com.cartisan", "com.example"};
        for (String pkg : packagesToScan) {
            scanBaseEnums(pkg);
        }
    }
}