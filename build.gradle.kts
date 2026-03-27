allprojects {
    group = "com.cartisan"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // cartisan-dependencies uses java-platform plugin, not java-library
    if (project.name != "cartisan-dependencies") {
        apply(plugin = "java-library")
        apply(plugin = "jacoco")
        apply(plugin = "maven-publish")
    }

    // Only configure Java toolchain for java-library projects
    if (project.name != "cartisan-dependencies") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }
    }

    // Only configure test dependencies for java-library projects
    if (project.name != "cartisan-dependencies") {
        dependencies {
            "testImplementation"(platform(project(":cartisan-dependencies")))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // Jacoco 配置
        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        // ========== Maven 发布配置 ==========
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("Cartisan Boot - ${project.name}")
                        url.set("https://github.com/cartisan-boot/cartisan-boot")

                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }

                        developers {
                            developer {
                                id.set("cartisan")
                                name.set("Cartisan Team")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/cartisan-boot/cartisan-boot.git")
                            developerConnection.set("scm:git:ssh://github.com/cartisan-boot/cartisan-boot.git")
                            url.set("https://github.com/cartisan-boot/cartisan-boot")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "local"
                    url = uri("${rootProject.buildDir}/local-maven-repo")
                }
            }
        }
    }
}
