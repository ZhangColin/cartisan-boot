package com.cartisan.web.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;

import java.util.Arrays;

/**
 * 类路径扫描根包的统一解析：显式配置优先，缺省取应用主包。
 *
 * <p>cartisan-web 中需要在启动期做类路径扫描的能力（{@code EnumScanner}、
 * {@code CodeMessageRegistry}）共用本解析器，保证缺省策略一致：</p>
 * <ol>
 *   <li>显式配置（如 {@code cartisan.web.enum-controller.scan-packages}）非空时优先，
 *       空白项自动剔除；</li>
 *   <li>未配置时取应用主包——{@code @SpringBootApplication} 所在包经
 *       {@link AutoConfigurationPackages} 注册，任意包名的服务均零配置生效；</li>
 *   <li>二者皆无（非 Boot 环境直接装配）回退 {@code com.cartisan}，
 *       至少覆盖框架自带枚举。</li>
 * </ol>
 *
 * @since 0.2.0
 */
public final class ScanPackageResolver {

    private static final String[] FALLBACK_PACKAGES = {"com.cartisan"};

    private ScanPackageResolver() {
    }

    /**
     * 解析有效扫描包。
     *
     * @param configured  显式配置的包（可为 null / 空数组 / 含空白项）
     * @param beanFactory BeanFactory（可为 null，用于查询 {@link AutoConfigurationPackages}）
     * @return 有效扫描包数组，永不返回 null 或空数组
     */
    public static String[] resolve(String[] configured, BeanFactory beanFactory) {
        String[] nonBlank = nonBlank(configured);
        if (nonBlank.length > 0) {
            return nonBlank;
        }
        if (beanFactory != null && AutoConfigurationPackages.has(beanFactory)) {
            return AutoConfigurationPackages.get(beanFactory).toArray(String[]::new);
        }
        return FALLBACK_PACKAGES;
    }

    private static String[] nonBlank(String[] packages) {
        if (packages == null) {
            return new String[0];
        }
        return Arrays.stream(packages)
            .filter(pkg -> pkg != null && !pkg.isBlank())
            .toArray(String[]::new);
    }
}
