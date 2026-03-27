plugins {
    java
    `java-library`
    // 版本由 settings.gradle.kts 统一管理
    id("info.solidsoft.pitest")
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    compileOnly(platform(project(":cartisan-dependencies")))

    // Spring stereotype 注解（compileOnly，不打包到 jar，由使用方提供）
    compileOnly("org.springframework:spring-context")

    // 测试依赖
    testImplementation(platform(project(":cartisan-dependencies")))
    testImplementation("org.springframework:spring-context")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.tngtech.archunit:archunit")
    // Pitest JUnit 5 插件（版本由 cartisan-dependencies 管理）
    testImplementation("org.pitest:pitest-junit5-plugin")

    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(project(":cartisan-dependencies")))
    annotationProcessor("org.projectlombok:lombok")
}

// ========== JavaDoc 校验配置 (F01-06) ==========

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        // -Xdoclint:all 启用所有检查
        // -missing 允许缺少文档（只检查格式，不强制必须存在）
        // -quiet 减少输出噪音
        addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}

// 确保 javadoc 在 build 时执行
tasks.build {
    dependsOn(tasks.javadoc)
}

// ========== PIT 配置 ==========

pitest {
    targetClasses.set(listOf("com.cartisan.core.domain.*"))
    targetTests.set(listOf("com.cartisan.core.domain.*", "com.cartisan.core.arch.*"))
    mutationThreshold.set(70)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    // 版本由 cartisan-dependencies 统一管理（1.15.0）
    // pitest 插件会自动从依赖中获取版本
}
