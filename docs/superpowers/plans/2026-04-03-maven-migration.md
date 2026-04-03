# cartisan-boot Gradle 到 Maven 迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 cartisan-boot 项目从 Gradle (Kotlin DSL) 完全迁移到 Maven，保留所有构建特性并确保发布到 Maven 仓库的 POM 文件完整。

**架构:** 创建父 POM 统一管理所有子模块和依赖版本，cartisan-dependencies 模块作为 BOM，按依赖顺序逐模块迁移 POM 配置，最后清理所有 Gradle 文件。

**Tech Stack:** Maven 3.9+、Java 21、Spring Boot 3.4.0、PIT、JaCoCo

---

## 文件结构总览

**新建文件:**
- `pom.xml` - 父 POM，定义全局配置和依赖版本管理
- `cartisan-dependencies/pom.xml` - BOM 模块，声明所有依赖版本
- `cartisan-core/pom.xml` - 核心模块（含 PIT、JavaDoc 校验）
- `cartisan-web/pom.xml` - Web 模块（含 Lombok + MapStruct 注解处理器链）
- `cartisan-event/pom.xml` - 事件模块
- `cartisan-data-jpa/pom.xml` - JPA 持久化模块
- `cartisan-test/pom.xml` - 测试工具模块（含 --add-opens JVM 参数）
- `cartisan-security/pom.xml` - 安全模块
- `cartisan-data-query/pom.xml` - jOOQ 查询模块
- `cartisan-ai/pom.xml` - AI 模块

**删除文件:**
- `build.gradle.kts` - 根项目 Gradle 配置
- `settings.gradle.kts` - Gradle 设置
- `gradle/` - Gradle wrapper 目录
- 所有子模块的 `build.gradle.kts`
- `.gradle/` - Gradle 缓存目录

---

## Task 1: 准备迁移环境

**Files:**
- None

- [ ] **Step 1: 创建 Git 分支**

```bash
git checkout -b feature/maven-migration
```

Expected: Switched to new branch 'feature/maven-migration'

- [ ] **Step 2: 运行基准测试确保当前状态正常**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 备份 Gradle 配置到文档**

创建 `docs/build/gradle-config-backup.md`，记录当前 Gradle 配置要点：

```bash
cat > docs/build/gradle-config-backup.md << 'EOF'
# Gradle Configuration Backup

**Date:** 2026-04-03
**Reason:** Maven Migration

## Key Gradle Features to Preserve

1. **Java Platform**: cartisan-dependencies uses `java-platform` plugin
2. **Java 21**: All modules use Java 21 toolchain
3. **Compiler Args**: `-parameters` flag (preserve parameter names)
4. **Annotation Processors**: Lombok → MapStruct → lombok-mapstruct-binding
5. **Testing**: JUnit 5 platform, JaCoCo coverage
6. **PIT**: cartisan-core has mutation testing (70% threshold)
7. **JavaDoc**: cartisan-core has strict validation (Xdoclint:all,-missing)
8. **Publishing**: Maven local repository at build/local-maven-repo

## Module Dependencies

cartisan-dependencies (BOM)
  → cartisan-core
    → cartisan-web
      → cartisan-security / cartisan-data-jpa / cartisan-event
        → cartisan-data-query / cartisan-ai

  → cartisan-test (depends on cartisan-core)
EOF
```

Expected: File created successfully

- [ ] **Step 4: 提交准备工作的 commit**

```bash
git add docs/build/gradle-config-backup.md
git commit -m "docs: backup Gradle configuration before Maven migration"
```

Expected: commit created

---

## Task 2: 创建父 POM

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 创建根目录 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.cartisan</groupId>
    <artifactId>cartisan-boot</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Cartisan Boot</name>
    <description>Business-agnostic Java framework based on DDD hexagonal architecture</description>
    <url>https://github.com/cartisan-boot/cartisan-boot</url>

    <licenses>
        <license>
            <name>The Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>

    <developers>
        <developer>
            <id>cartisan</id>
            <name>Cartisan Team</name>
        </developer>
    </developers>

    <scm>
        <connection>scm:git:git://github.com/cartisan-boot/cartisan-boot.git</connection>
        <developerConnection>scm:git:ssh://github.com/cartisan-boot/cartisan-boot.git</developerConnection>
        <url>https://github.com/cartisan-boot/cartisan-boot</url>
    </scm>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>

        <!-- Dependency versions -->
        <spring-boot.version>3.4.0</spring-boot.version>
        <springdoc.version>2.7.0</springdoc.version>
        <flyway.version>10.18.0</flyway.version>
        <jooq.version>3.19.29</jooq.version>
        <sa-token.version>1.45.0</sa-token.version>
        <lombok.version>1.18.34</lombok.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <assertj.version>3.26.3</assertj.version>
        <archunit.version>1.3.0</archunit.version>
        <wiremock.version>3.2.0</wiremock.version>
        <fastjson2.version>2.0.53</fastjson2.version>
        <junit.version>5.11.4</junit.version>
        <hutool.version>5.8.34</hutool.version>
        <druid.version>1.2.23</druid.version>
        <pitest.version>1.19.0</pitest.version>
        <pitest-junit5-plugin.version>1.2.3</pitest-junit5-plugin.version>
    </properties>

    <modules>
        <module>cartisan-dependencies</module>
        <module>cartisan-core</module>
        <module>cartisan-web</module>
        <module>cartisan-test</module>
        <module>cartisan-security</module>
        <module>cartisan-data-jpa</module>
        <module>cartisan-event</module>
        <module>cartisan-data-query</module>
        <module>cartisan-ai</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- jOOQ BOM -->
            <dependency>
                <groupId>org.jooq</groupId>
                <artifactId>jooq-bom</artifactId>
                <version>${jooq.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- JUnit BOM -->
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>${junit.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- SpringDoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>

            <!-- Flyway -->
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-core</artifactId>
                <version>${flyway.version}</version>
            </dependency>

            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>

            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- MapStruct Processor -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- Lombok + MapStruct Integration -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>${lombok-mapstruct-binding.version}</version>
            </dependency>

            <!-- AssertJ -->
            <dependency>
                <groupId>org.assertj</groupId>
                <artifactId>assertj-core</artifactId>
                <version>${assertj.version}</version>
            </dependency>

            <!-- ArchUnit -->
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit</artifactId>
                <version>${archunit.version}</version>
            </dependency>

            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
            </dependency>

            <!-- WireMock -->
            <dependency>
                <groupId>org.wiremock.integrations</groupId>
                <artifactId>wiremock-spring-boot</artifactId>
                <version>${wiremock.version}</version>
            </dependency>

            <!-- Fastjson2 -->
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2.version}</version>
            </dependency>

            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- PIT JUnit 5 Plugin -->
            <dependency>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-junit5-plugin</artifactId>
                <version>${pitest-junit5-plugin.version}</version>
            </dependency>

            <!-- Druid -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>

            <!-- Mockito -->
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>5.14.2</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- Maven Compiler Plugin -->
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <source>21</source>
                        <target>21</target>
                        <parameters>true</parameters>
                    </configuration>
                </plugin>

                <!-- Maven Surefire Plugin (Unit Tests) -->
                <plugin>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.5.2</version>
                    <configuration>
                        <argLine>--add-opens java.base/java.lang=ALL-UNNAMED</argLine>
                    </configuration>
                </plugin>

                <!-- JaCoCo Code Coverage -->
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>0.8.12</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>prepare-agent</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>report</id>
                            <phase>test</phase>
                            <goals>
                                <goal>report</goal>
                            </goals>
                        </execution>
                    </executions>
                    <configuration>
                        <formats>
                            <format>XML</format>
                            <format>HTML</format>
                        </formats>
                    </configuration>
                </plugin>

                <!-- Maven Deploy Plugin -->
                <plugin>
                    <artifactId>maven-deploy-plugin</artifactId>
                    <version>3.1.3</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <distributionManagement>
        <repository>
            <id>local</id>
            <url>file://${project.build.directory}/local-maven-repo</url>
        </repository>
    </distributionManagement>
</project>
```

Expected: File created at `/Users/zhangcolin/workspace/cartisan-boot/pom.xml`

- [ ] **Step 2: 验证 POM 语法**

```bash
mvn help:effective-pom
```

Expected: No errors, effective POM displayed

- [ ] **Step 3: 提交父 POM**

```bash
git add pom.xml
git commit -m "feat: add parent POM with dependency management"
```

Expected: commit created

---

## Task 3: 创建 cartisan-dependencies BOM 模块

**Files:**
- Create: `cartisan-dependencies/pom.xml`

- [ ] **Step 1: 创建 BOM 模块 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-dependencies</artifactId>
    <packaging>pom</packaging>

    <name>Cartisan Dependencies</name>
    <description>Cartisan Boot - Dependency Management BOM</description>
</project>
```

Expected: File created at `cartisan-dependencies/pom.xml`

- [ ] **Step 2: 验证 BOM 模块编译**

```bash
mvn clean install -pl cartisan-dependencies
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 验证 BOM 文件内容**

```bash
cat ~/.m2/repository/com/cartisan/cartisan-dependencies/0.1.0-SNAPSHOT/cartisan-dependencies-0.1.0-SNAPSHOT.pom | grep -A 5 "dependencyManagement"
```

Expected: BOM file contains dependencyManagement section

- [ ] **Step 4: 提交 BOM 模块**

```bash
git add cartisan-dependencies/pom.xml
git commit -m "feat: add cartisan-dependencies BOM module"
```

Expected: commit created

---

## Task 4: 迁移 cartisan-core 模块

**Files:**
- Create: `cartisan-core/pom.xml`

- [ ] **Step 1: 创建 cartisan-core pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-core</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Core</name>
    <description>Cartisan Boot - Core Domain Module</description>

    <dependencies>
        <!-- Spring (compileOnly, not packaged) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- JavaDoc Validation -->
            <plugin>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.11.2</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <doclint>all,-missing</doclint>
                    <quiet>true</quiet>
                </configuration>
            </plugin>

            <!-- PIT Mutation Testing -->
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <version>${pitest.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                        <version>${pitest-junit5-plugin.version}</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <targetClasses>
                        <param>com.cartisan.core.domain.*</param>
                    </targetClasses>
                    <targetTests>
                        <param>com.cartisan.core.domain.*</param>
                        <param>com.cartisan.core.arch.*</param>
                    </targetTests>
                    <mutationThreshold>70</mutationThreshold>
                    <outputFormats>
                        <format>HTML</format>
                        <format>XML</format>
                    </outputFormats>
                    <timestampedReports>false</timestampedReports>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Expected: File created at `cartisan-core/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-core**

```bash
mvn clean test -pl cartisan-core
```

Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 3: 运行 PIT 变异测试**

```bash
mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core
```

Expected: Mutation testing report generated, coverage ≥ 70%

- [ ] **Step 4: 验证 JavaDoc 校验**

```bash
mvn javadoc:javadoc -pl cartisan-core
```

Expected: No JavaDoc errors (doclint passes)

- [ ] **Step 5: 提交 cartisan-core 迁移**

```bash
git add cartisan-core/pom.xml
git commit -m "feat: migrate cartisan-core to Maven"
```

Expected: commit created

---

## Task 5: 迁移 cartisan-web 模块

**Files:**
- Create: `cartisan-web/pom.xml`

- [ ] **Step 1: 创建 cartisan-web pom.xml（含 Lombok + MapStruct 链）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-web</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Web</name>
    <description>Cartisan Boot - Web Module</description>

    <dependencies>
        <!-- cartisan-core -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring AOP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Spring Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Data Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Fastjson2 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin with annotation processors -->
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <parameters>true</parameters>
                    <annotationProcessorPaths>
                        <!-- 1. Lombok (first) -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <!-- 2. MapStruct Processor (second) -->
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <!-- 3. Lombok-MapStruct-Binding (third, integration layer) -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Expected: File created at `cartisan-web/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-web**

```bash
mvn clean test -pl cartisan-web
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 验证 MapStruct 代码生成**

```bash
ls -la cartisan-web/target/generated-sources/annotations/com/cartisan/web/mapper/
```

Expected: MapStruct mapper implementations generated

- [ ] **Step 4: 提交 cartisan-web 迁移**

```bash
git add cartisan-web/pom.xml
git commit -m "feat: migrate cartisan-web to Maven with MapStruct"
```

Expected: commit created

---

## Task 6: 迁移 cartisan-event 模块

**Files:**
- Create: `cartisan-event/pom.xml`

- [ ] **Step 1: 创建 cartisan-event pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-event</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Event</name>
    <description>Cartisan Boot - Domain Event Module</description>

    <dependencies>
        <!-- cartisan-core -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Context -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>

        <!-- Spring Boot AutoConfigure -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Expected: File created at `cartisan-event/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-event**

```bash
mvn clean test -pl cartisan-event
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 cartisan-event 迁移**

```bash
git add cartisan-event/pom.xml
git commit -m "feat: migrate cartisan-event to Maven"
```

Expected: commit created

---

## Task 7: 迁移 cartisan-data-jpa 模块

**Files:**
- Create: `cartisan-data-jpa/pom.xml`

- [ ] **Step 1: 创建 cartisan-data-jpa pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-data-jpa</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Data JPA</name>
    <description>Cartisan Boot - JPA Persistence Module</description>

    <dependencies>
        <!-- cartisan-core -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- cartisan-event -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-event</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Druid (optional, compileOnly) -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Expected: File created at `cartisan-data-jpa/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-data-jpa**

```bash
mvn clean test -pl cartisan-data-jpa
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 cartisan-data-jpa 迁移**

```bash
git add cartisan-data-jpa/pom.xml
git commit -m "feat: migrate cartisan-data-jpa to Maven"
```

Expected: commit created

---

## Task 8: 迁移 cartisan-test 模块

**Files:**
- Create: `cartisan-test/pom.xml`

- [ ] **Step 1: 创建 cartisan-test pom.xml（含 --add-opens JVM 参数）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-test</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Test</name>
    <description>Cartisan Boot - Testing Utilities Module</description>

    <dependencies>
        <!-- cartisan-core -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Test frameworks (exported to consumers) -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
        </dependency>

        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit</artifactId>
        </dependency>

        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-test</artifactId>
        </dependency>

        <!-- Spring Test -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-test</artifactId>
        </dependency>

        <!-- Spring Boot Starter Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring JDBC -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Surefire plugin with --add-opens for internal API access -->
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Expected: File created at `cartisan-test/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-test**

```bash
mvn clean test -pl cartisan-test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 cartisan-test 迁移**

```bash
git add cartisan-test/pom.xml
git commit -m "feat: migrate cartisan-test to Maven with --add-opens"
```

Expected: commit created

---

## Task 9: 迁移 cartisan-security 模块

**Files:**
- Create: `cartisan-security/pom.xml`

- [ ] **Step 1: 创建 cartisan-security pom.xml（删除 --enable-preview）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-security</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Security</name>
    <description>Cartisan Boot - Security Module</description>

    <dependencies>
        <!-- cartisan-core -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- cartisan-web -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-web</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Sa-Token -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
        </dependency>

        <!-- Spring Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Expected: File created at `cartisan-security/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-security**

```bash
mvn clean test -pl cartisan-security
```

Expected: BUILD SUCCESSFUL (Java Record works without --enable-preview)

- [ ] **Step 3: 提交 cartisan-security 迁移**

```bash
git add cartisan-security/pom.xml
git commit -m "feat: migrate cartisan-security to Maven (no preview needed)"
```

Expected: commit created

---

## Task 10: 迁移 cartisan-data-query 模块

**Files:**
- Create: `cartisan-data-query/pom.xml`

- [ ] **Step 1: 创建 cartisan-data-query pom.xml（删除 --enable-preview）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-data-query</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan Data Query</name>
    <description>Cartisan Boot - jOOQ Query Module</description>

    <dependencies>
        <!-- cartisan-web -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-web</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- jOOQ Core -->
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq</artifactId>
        </dependency>

        <!-- Spring Boot AutoConfiguration -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- cartisan-security (optional, compileOnly for JooqTenantSupport) -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-security</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-security</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-test</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Configuration Processor -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Expected: File created at `cartisan-data-query/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-data-query**

```bash
mvn clean test -pl cartisan-data-query
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 cartisan-data-query 迁移**

```bash
git add cartisan-data-query/pom.xml
git commit -m "feat: migrate cartisan-data-query to Maven (no preview needed)"
```

Expected: commit created

---

## Task 11: 迁移 cartisan-ai 模块

**Files:**
- Create: `cartisan-ai/pom.xml`

- [ ] **Step 1: 创建 cartisan-ai pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>cartisan-ai</artifactId>
    <packaging>jar</packaging>

    <name>Cartisan AI</name>
    <description>Cartisan Boot - AI Integration Module</description>

    <dependencies>
        <!-- cartisan-core -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring WebFlux -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webflux</artifactId>
        </dependency>

        <!-- Spring WebMVC (for SSE) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
        </dependency>

        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Reactor Netty HTTP -->
        <dependency>
            <groupId>io.projectreactor.netty</groupId>
            <artifactId>reactor-netty-http</artifactId>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok (compile time) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.wiremock.integrations</groupId>
            <artifactId>wiremock-spring-boot</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Expected: File created at `cartisan-ai/pom.xml`

- [ ] **Step 2: 编译并测试 cartisan-ai**

```bash
mvn clean test -pl cartisan-ai
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交 cartisan-ai 迁移**

```bash
git add cartisan-ai/pom.xml
git commit -m "feat: migrate cartisan-ai to Maven"
```

Expected: commit created

---

## Task 12: 全量构建测试

**Files:**
- None

- [ ] **Step 1: 清理并全量构建**

```bash
mvn clean install
```

Expected: BUILD SUCCESSFUL, all modules compiled and tested

- [ ] **Step 2: 生成 JaCoCo 覆盖率报告**

```bash
mvn jacoco:report
```

Expected: JaCoCo reports generated in target/site/jacoco/

- [ ] **Step 3: 验证所有 POM 文件**

```bash
find ~/.m2/repository/com/cartisan -name "*.pom" -exec echo "=== {} ===" \; -exec head -20 {} \;
```

Expected: All POM files contain proper dependency versions

- [ ] **Step 4: 提交全量测试通过**

```bash
git add .
git commit -m "test: all modules pass Maven build and tests"
```

Expected: commit created

---

## Task 13: 清理 Gradle 文件

**Files:**
- Delete: `build.gradle.kts`
- Delete: `settings.gradle.kts`
- Delete: `gradle/` directory
- Delete: all `*/build.gradle.kts`

- [ ] **Step 1: 删除根目录 Gradle 文件**

```bash
rm -f build.gradle.kts
rm -f settings.gradle.kts
rm -rf gradle/
rm -rf .gradle/
```

Expected: Files deleted

- [ ] **Step 2: 删除所有子模块的 Gradle 文件**

```bash
find . -name "build.gradle.kts" -type f -delete
```

Expected: All build.gradle.kts files deleted

- [ ] **Step 3: 验证无残留 Gradle 文件**

```bash
find . -name "*.gradle*" -o -name "gradlew*" | grep -v ".git"
```

Expected: No Gradle files found (except in .git history)

- [ ] **Step 4: 提交清理**

```bash
git add -A
git commit -m "chore: remove all Gradle files and configuration"
```

Expected: commit created

---

## Task 14: 最终验证和发布

**Files:**
- None

- [ ] **Step 1: 最终全量测试**

```bash
mvn clean test
```

Expected: All tests pass

- [ ] **Step 2: 安装到本地 Maven 仓库**

```bash
mvn clean install
```

Expected: BUILD SUCCESSFUL, JARs installed to ~/.m2/repository/com/cartisan/

- [ ] **Step 3: 验证 POM 文件包含所有依赖版本**

```bash
# 检查 cartisan-web 的 POM（之前问题模块）
unzip -p ~/.m2/repository/com/cartisan/cartisan-web/0.1.0-SNAPSHOT/cartisan-web-0.1.0-SNAPSHOT.jar META-INF/maven/com.cartisan/cartisan-web/pom.xml | grep -A 2 "fastjson2"
```

Expected: POM contains `<version>2.0.53</version>` for fastjson2

- [ ] **Step 4: 创建简单的下游测试项目**

```bash
cd /tmp
mkdir -p maven-test-project/src/main/java/com/test
cd maven-test-project

cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.test</groupId>
    <artifactId>maven-test-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-web</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
EOF

cat > src/main/java/com/test/App.java << 'EOF'
package com.test;

import com.cartisan.web.response.PageResponse;
import com.cartisan.core.domain.Identity;

public class App {
    public static void main(String[] args) {
        System.out.println("Maven dependency test: " + PageResponse.class.getName());
        System.out.println("Maven dependency test: " + Identity.class.getName());
    }
}
EOF

mvn compile
```

Expected: BUILD SUCCESSFUL, downstream project can resolve all dependencies

- [ ] **Step 5: 运行 PIT 变异测试验证**

```bash
cd /Users/zhangcolin/workspace/cartisan-boot
mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core
```

Expected: PIT report shows ≥ 70% mutation coverage

- [ ] **Step 6: 提交最终验证**

```bash
git add .
git commit -m "test: verify Maven migration complete and downstream compatible"
```

Expected: commit created

---

## Task 15: 更新文档

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: 更新 README.md 构建命令**

查找并替换 Gradle 命令为 Maven 命令：

```bash
# 在 README.md 中替换：
# ./gradlew compileJava → mvn compile
# ./gradlew test → mvn test
# ./gradlew build → mvn package
# ./gradlew :模块名:test → mvn test -pl 模块名
# ./gradlew publishToMavenLocal → mvn install
```

Expected: README.md updated with Maven commands

- [ ] **Step 2: 更新 CLAUDE.md 常用命令**

```bash
# 在 CLAUDE.md 中替换常用命令部分
cat > /tmp/maven-commands.txt << 'EOF'
## 常用命令

- 编译：`mvn compile`
- 单元测试：`mvn test -pl 模块名`
- 全量测试：`mvn test`
- 打包：`mvn package`
- 安装到本地仓库：`mvn install`
- 变异测试：`mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core`
EOF
# 手动将上述内容添加到 CLAUDE.md
```

Expected: CLAUDE.md updated with Maven commands

- [ ] **Step 3: 添加 Maven 相关 PITFALLS（如果发现新坑）**

如果有新的 Maven 相关踩坑经验，添加到 `docs/PITFALLS.md`

- [ ] **Step 4: 提交文档更新**

```bash
git add README.md CLAUDE.md docs/PITFALLS.md
git commit -m "docs: update build commands from Gradle to Maven"
```

Expected: commit created

---

## Task 16: 合并到主分支

**Files:**
- None

- [ ] **Step 1: 切换到主分支**

```bash
git checkout develop
```

Expected: Switched to branch 'develop'

- [ ] **Step 2: 合并迁移分支**

```bash
git merge feature/maven-migration
```

Expected: Merge successful, no conflicts

- [ ] **Step 3: 打 Tag**

```bash
git tag -a v0.1.0-maven -m "Maven migration - first Maven-based release"
```

Expected: Tag created

- [ ] **Step 4: 推送到远程（如果需要）**

```bash
git push origin develop
git push origin v0.1.0-maven
```

Expected: Pushed to remote (if configured)

---

## 验收标准检查清单

完成所有任务后，验证以下标准：

- [ ] 所有单元测试通过 (`mvn test`)
- [ ] `mvn clean install` 成功执行
- [ ] PIT 变异测试杀死率 ≥ 70% (cartisan-core)
- [ ] JaCoCo 覆盖率报告生成
- [ ] 发布的 POM 文件包含所有依赖版本号
- [ ] 下游测试项目可以通过 Maven 仓库正常依赖
- [ ] README.md 和 CLAUDE.md 中的命令已更新为 Maven

---

## 故障排查指南

### 问题 1: 注解处理器顺序错误

**症状**: 编译失败，提示 Lombok 或 MapStruct 相关错误

**解决**: 检查 `cartisan-web/pom.xml` 中的 `<annotationProcessorPaths>` 顺序：
1. Lombok (first)
2. mapstruct-processor (second)
3. lombok-mapstruct-binding (third)

### 问题 2: PIT 插件版本冲突

**症状**: PIT 测试失败，ClassNotFoundException

**解决**: 确认 `pitest.version=1.19.0` 和 `pitest-junit5-plugin.version=1.2.3` 版本匹配

### 问题 3: 依赖版本缺失

**症状**: 下游项目无法解析依赖版本

**解决**:
1. 检查父 POM 的 `<dependencyManagement>` 是否包含该依赖
2. 检查 BOM 模块是否正确引用父 POM
3. 运行 `mvn dependency:tree` 检查依赖树

### 问题 4: JavaDoc 校验失败

**症状**: cartisan-core 编译时 JavaDoc 报错

**解决**:
1. 检查 `<doclint>all,-missing</doclint>` 配置
2. 检查 JavaDoc 注释格式
3. 临时跳过: `mvn install -Dmaven.javadoc.skip=true` (仅用于调试)

---

## 下一步工作

- [ ] 业务平台迁移到 Maven
- [ ] CI/CD 流程更新（使用 Maven 替代 Gradle）
- [ ] 团队培训：Maven 最佳实践

---

**计划完成时间估计**: 2-3 小时（假设所有测试通过）
**风险级别**: 中等（主要风险在注解处理器和 PIT 插件兼容性）
