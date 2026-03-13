plugins {
    java
    `java-library`
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
}

dependencies {
    // 零外部依赖 - 仅使用 JDK 标准库

    // 测试依赖
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.tngtech.archunit:archunit:1.3.0")
}

pitest {
    targetClasses.set(listOf("com.cartisan.core.domain.*"))
    targetTests.set(listOf("com.cartisan.core.domain.*", "com.cartisan.core.arch.*"))
    mutationThreshold.set(70)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    junit5PluginVersion.set("1.2.1")
}
