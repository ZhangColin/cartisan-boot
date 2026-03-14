plugins {
    java
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    // cartisan-core - provides AggregateRoot interface
    api(project(":cartisan-core"))

    // Spring Data JPA - provides JpaRepository, JpaSpecificationExecutor
    api("org.springframework.boot:spring-boot-starter-data-jpa")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}
