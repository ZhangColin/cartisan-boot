plugins {
    `java-platform`
}

dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // jOOQ BOM（F04-01）
    api(platform("org.jooq:jooq-bom:3.19.29"))

    // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")

    // Lombok - 框架内置，减少样板代码
    api("org.projectlombok:lombok:1.18.34")

    // MapStruct - 类型安全的对象映射
    api("org.mapstruct:mapstruct:1.6.3")

    // Lombok + MapStruct 集成
    api("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}

javaPlatform {
    allowDependencies()
}
