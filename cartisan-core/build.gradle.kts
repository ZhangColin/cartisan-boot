plugins {
    java
    `java-library`
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
}

dependencies {
    // Spring stereotype 注解（compileOnly，不打包到 jar，由使用方提供）
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    compileOnly("org.springframework:spring-context")

    // 测试依赖
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    testImplementation("org.springframework:spring-context")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.tngtech.archunit:archunit:1.3.0")
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
    junit5PluginVersion.set("1.2.1")
}
