plugins {
    `java-platform`
}

dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
}

javaPlatform {
    allowDependencies()
}
