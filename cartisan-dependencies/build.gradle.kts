plugins {
    `java-platform`
}

dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    constraints {
    }
}

javaPlatform {
    allowDependencies()
}
