# cartisan-boot Gradle 到 Maven 迁移设计

**日期**: 2026-04-03
**作者**: Claude Sonnet 4.6
**状态**: 待审批

## 1. 背景

cartisan-boot 当前使用 Gradle (Kotlin DSL) 作为构建工具，但在实际使用中遇到以下问题：

- **POM 生成问题**: Gradle 发布到 Maven 仓库时，生成的 POM 文件缺少依赖版本号，导致下游项目无法解析依赖
- **生态兼容性**: 外部团队主要使用 Maven，Gradle-Maven 互操作性存在额外成本
- **学习曲线**: 团队已经多次尝试 Gradle，但持续遇到各种配置问题

因此决定将 cartisan-boot 从 Gradle 完全迁移到 Maven。

## 2. 目标

1. **完整保留现有功能**: 所有 Gradle 构建特性必须在 Maven 中等价实现
2. **保持源码历史**: Java 源码文件保留 Git 历史
3. **零业务变更**: 不修改任何业务代码和测试代码
4. **兼容性**: 发布的 JAR 包和 POM 文件与下游项目完全兼容

## 3. 架构设计

### 3.1 项目结构

```
cartisan-boot/
├── pom.xml (父 POM)
├── cartisan-dependencies/
│   └── pom.xml (BOM)
├── cartisan-core/
│   ├── pom.xml
│   └── src/
├── cartisan-web/
│   ├── pom.xml
│   └── src/
├── cartisan-test/
│   ├── pom.xml
│   └── src/
├── cartisan-security/
│   ├── pom.xml
│   └── src/
├── cartisan-data-jpa/
│   ├── pom.xml
│   └── src/
├── cartisan-event/
│   ├── pom.xml
│   └── src/
├── cartisan-data-query/
│   ├── pom.xml
│   └── src/
└── cartisan-ai/
    ├── pom.xml
    └── src/
```

### 3.2 父 POM 设计

**核心职责**:
- 统一管理所有子模块
- 定义全局属性（版本号、编码、Java 版本）
- 配置公共插件（compiler、surefire、jacoco）
- 定义依赖版本管理（`<dependencyManagement>`）

**关键配置**:

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>

    <!-- 依赖版本 -->
    <spring-boot.version>3.4.0</spring-boot.version>
    <springdoc.version>2.7.0</springdoc.version>
    <flyway.version>10.18.0</flyway.version>
    <jooq.version>3.19.29</jooq.version>
    <sa-token.version>1.45.0</sa-token.version>
    <lombok.version>1.18.34</lombok.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <assertj.version>3.26.3</assertj.version>
    <archunit.version>1.3.0</archunit.version>
    <wiremock.version>3.2.0</wiremock.version>
    <fastjson2.version>2.0.53</fastjson2.version>
    <junit.version>5.11.4</junit.version>
    <hutool.version>5.8.34</hutool.version>
    <druid.version>1.2.23</druid.version>
    <pitest.version>1.19.0</pitest.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- 其他依赖... -->
    </dependencies>
</dependencyManagement>
```

### 3.3 BOM 模块设计 (cartisan-dependencies)

**对应 Gradle**: `java-platform` 插件

**Maven 实现**:
- `<packaging>pom</packaging>`
- 使用 `<dependencyManagement>` 声明所有依赖版本
- 不包含任何代码

**关键依赖清单**:

1. Spring Boot BOM 3.4.0
2. SpringDoc OpenAPI 2.7.0
3. Flyway 10.18.0
4. jOOQ BOM 3.19.29
5. Sa-Token 1.45.0
6. Lombok 1.18.34
7. MapStruct 1.6.3
8. AssertJ 3.26.3
9. ArchUnit 1.3.0
10. WireMock 3.2.0
11. Fastjson2 2.0.53
12. JUnit BOM 5.11.4
13. Hutool 5.8.34
14. PIT Maven Plugin 1.19.0
15. PIT JUnit 5 Plugin 1.2.3
16. Druid 1.2.23

### 3.4 模块间依赖关系

```
cartisan-dependencies (BOM)
    ↓
cartisan-core
    ↓
cartisan-web
    ↓
cartisan-security / cartisan-data-jpa / cartisan-event
    ↓
cartisan-data-query / cartisan-ai
```

**特殊情况**:
- `cartisan-test`: 依赖 `cartisan-core`
- `cartisan-data-query`: 依赖 `cartisan-web`（复用 PageResponse）

### 3.5 编译器配置

**Java 21**:
- 所有模块统一使用 Java 21
- 编译参数: `-parameters`（保留参数名）

**注解处理器链**（cartisan-web）:
1. Lombok
2. MapStruct Processor
3. Lombok-MapStruct-Binding（集成层）

**Maven 配置**:
```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <parameters>true</parameters>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3.6 测试配置

**JUnit 5 平台**:
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--add-opens java.base/java.lang=ALL-UNNAMED</argLine>
    </configuration>
</plugin>
```

**JaCoCo 代码覆盖率**:
- 所有模块启用
- 生成 XML 和 HTML 报告

### 3.7 特殊模块配置

#### cartisan-core
- **PIT 变异测试**: 使用 `pitest-maven` 插件
- **JavaDoc 校验**: 编译时强制执行（Xdoclint:all,-missing）
- **配置**:
  ```xml
  <plugin>
      <artifactId>maven-javadoc-plugin</artifactId>
      <configuration>
          <doclint>all,-missing</doclint>
      </configuration>
  </plugin>
  ```

#### cartisan-test
- **JVM 参数**: `--add-opens java.base/java.lang=ALL-UNNAMED`（访问内部 API）

#### cartisan-security 和 cartisan-data-query
- **删除**: `--enable-preview` 参数（Java Record 是正式特性，不需要预览模式）

### 3.8 发布配置

**Maven Deploy Plugin**:
```xml
<plugin>
    <artifactId>maven-deploy-plugin</artifactId>
</plugin>

<distributionManagement>
    <repository>
        <id>local</id>
        <url>file://${project.build.directory}/local-maven-repo</url>
    </repository>
</distributionManagement>
```

**POM 元数据**:
- License: Apache 2.0
- Developers: Cartisan Team
- SCM: GitHub repository URL

## 4. 迁移步骤

### 4.1 准备阶段
1. 创建 Git 分支 `feature/maven-migration`
2. 备份当前 Gradle 配置（文档化）
3. 运行完整测试套件确保基准状态

### 4.2 实施阶段
1. 创建父 POM (`pom.xml`)
2. 创建 cartisan-dependencies BOM
3. 逐模块迁移（按依赖顺序）:
   - cartisan-core
   - cartisan-web
   - cartisan-event
   - cartisan-data-jpa
   - cartisan-test
   - cartisan-security
   - cartisan-data-query
   - cartisan-ai
4. 每个模块迁移后运行测试验证

### 4.3 清理阶段
1. 删除所有 `*.gradle.kts` 文件
2. 删除 `gradle/` 目录
3. 删除 `.gradle/` 目录
4. 删除 `settings.gradle.kts`
5. 删除 `build.gradle.kts`

### 4.4 验证阶段
1. 运行完整测试套件
2. 执行 `mvn clean install` 发布到本地仓库
3. 验证下游项目依赖（创建临时测试项目或使用现有业务平台）
4. PIT 变异测试（cartisan-core）

### 4.5 发布阶段
1. 合并到主分支
2. 打 Tag
3. 发布到远程 Maven 仓库（如需要）

## 5. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 注解处理器顺序错误 | 编译失败 | 严格按照 Lombok → MapStruct → Binding 顺序配置 |
| PIT 插件兼容性 | 变异测试失败 | 使用 `pitest-maven` 插件，验证版本兼容性 |
| 依赖版本冲突 | 运行时错误 | 使用 BOM 统一管理，通过 `mvn dependency:tree` 验证 |
| Git 历史丢失 | 无法追溯 | 使用 Git 重命名保留源码历史（如果需要） |

## 6. 验收标准

1. ✅ 所有单元测试通过
2. ✅ `mvn clean install` 成功执行
3. ✅ PIT 变异测试杀死率 ≥ 70%（cartisan-core）
4. ✅ JaCoCo 覆盖率报告生成
5. ✅ 发布的 POM 文件包含所有依赖版本号
6. ✅ 下游项目可以通过 Maven 仓库正常依赖

## 7. 后续工作

- [ ] 更新 README.md 中的构建命令
- [ ] 更新 CLAUDE.md 中的常用命令
- [ ] 更新 PITFALLS.md（如果有新的 Maven 相关坑）
- [ ] 业务平台迁移到 Maven

## 8. 附录

### 8.1 Gradle 特性到 Maven 映射表

| Gradle | Maven | 说明 |
|--------|-------|------|
| `java-platform` | `<packaging>pom</packaging>` + `<dependencyManagement>` | BOM 机制 |
| `api(platform(...))` | 在 BOM 中声明版本，子模块不写版本号 | 依赖版本管理 |
| `api(...)` | `<dependency>` + `<scope>compile</scope>` | 编译时依赖（Gradle 中会传递给消费者） |
| `implementation(...)` | `<dependency>` + `<scope>compile</scope>` | 实现依赖（Gradle 中不传递，但 Maven 无此区分） |
| `compileOnly(...)` | `<dependency>` + `<scope>provided</scope>` | 编译期依赖 |
| `testImplementation(...)` | `<dependency>` + `<scope>test</scope>` | 测试依赖 |
| `-parameters` | `<parameters>true</parameters>` | 保留参数名 |
| `java { toolchain { languageVersion = 21 } }` | `<maven.compiler.source>21</maven.compiler.source>` | Java 版本 |
| `annotationProcessor(...)` | `<annotationProcessorPath>` | 注解处理器 |

### 8.2 常用命令对照

| 操作 | Gradle | Maven |
|------|--------|-------|
| 编译 | `./gradlew compileJava` | `mvn compile` |
| 单元测试 | `./gradlew :模块名:test` | `mvn test -pl 模块名` |
| 全量测试 | `./gradlew test` | `mvn test` |
| 打包 | `./gradlew build` | `mvn package` |
| 安装到本地仓库 | `./gradlew publishToMavenLocal` | `mvn install` |
| 变异测试 | `./gradlew :cartisan-core:pitest` | `mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core` |
