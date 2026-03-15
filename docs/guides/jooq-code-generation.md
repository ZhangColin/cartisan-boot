# jOOQ 代码生成配置指南

> **适用范围**：本指南适用于使用 Gradle（Kotlin DSL）、Flyway、PostgreSQL 且已引入 `cartisan-dependencies` 的 Spring Boot 业务项目。

---

## 一、快速上手（复制即用）

### 1.1 最小化配置示例

在业务项目的 `build.gradle.kts` 中添加以下配置：

```kotlin
plugins {
    java
    id("nu.studer.jooq") version "8.2.1"
}

dependencies {
    // cartisan-dependencies BOM 管理 jOOQ 版本，无需显式指定
    api(platform(project(":cartisan-dependencies")))

    // jOOQ 代码生成器依赖
    jooqGenerator("org.jooq:jooq-codegen")
    jooqGenerator("org.jooq:jooq-meta")
    jooqGenerator("org.jooq:jooq-meta-extensions")

    // PostgreSQL JDBC（代码生成时需要连接数据库读取 schema）
    jooqGenerator("org.postgresql:postgresql")
}

jooq {
    version.set("3.19.29")  // 与 cartisan-dependencies BOM 保持一致

    configuration {
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                // 引用项目已有的数据库配置，避免重复配置
                url = project.findProperty("db.url") as String? ?: "jdbc:postgresql://localhost:5432/mydb"
                user = project.findProperty("db.user") as String? ?: "postgres"
                password = project.findProperty("db.password") as String? ?: "postgres"
                schema = "public"  // 生成 public schema 下的表
            }

            generate {
                // 使用 java.time 类型而非 java.sql
                isJavaTimeTypes = true
                // 生成 Record 和 DAO
                isRecords = true
                isPojos = false  // 使用 Record 即可，不需要 POJO
                isDaos = true
            }

            target {
                // 推荐包名：业务项目根包 + .db
                packageName = "com.example.db"
                // 输出目录
                directory = "build/generated/jooq"
            }
        }
    }
}

// 关键：先执行 Flyway 迁移，再生成 jOOQ 代码
tasks.named<nu.studer.jooq.GenerateJooqTask>("generateJooq") {
    dependsOn("flywayMigrate")
}

// 让 IDEA 识别生成的代码为源码目录
idea {
    module {
        generatedSourceDirs.add(file("build/generated/jooq"))
    }
}
```

### 1.2 验证生成

```bash
# 1. 先执行 Flyway 迁移（创建/更新表结构）
./gradlew flywayMigrate

# 2. 再生成 jOOQ 代码
./gradlew generateJooq

# 3. 验证生成结果
ls -la build/generated/jooq/com/example/db/tables/
```

生成后，你可以在代码中这样使用：

```java
import static com.example.db.Tables.*;
import com.example.db.tables.User;

// DSL 查询
List<UserRecord> users = dsl.selectFrom(USER)
    .where(USER.AGE.gt(18))
    .fetch();
```

### 1.3 IDE 配置（IDEA）

**问题**：生成后 IDEA 可能不识别 `build/generated/jooq` 目录，报 "Cannot find symbol"。

**解决**：

1. 方式一：通过 Gradle 同步
   ```bash
   ./gradlew cleanIdea idea
   # 然后在 IDEA 中重新打开项目或 Reload Gradle Project
   ```

2. 方式二：手动标记
   - 在 IDEA 中右键 `build/generated/jooq` 目录
   - 选择 `Mark Directory as` → `Generated Sources Root`

---

## 二、配置说明

### 2.1 jOOQ 插件

使用 `nu.studer.jooq` Gradle 插件（而非官方插件），原因：
- 更好的 Kotlin DSL 支持
- 与 Gradle 版本 catalog 集成更好
- 维护活跃，文档完善

```kotlin
id("nu.studer.jooq") version "8.2.1"
```

### 2.2 数据库连接（引用项目已有配置）

**原则**：代码生成时的数据库连接应与运行时保持一致，避免重复配置。

**推荐方式**：通过 `project.findProperty()` 引用：

```kotlin
// 方式一：使用 gradle.properties
url = project.findProperty("db.url") as String? ?: "jdbc:postgresql://localhost:5432/mydb"

// 方式二：使用环境变量
url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/mydb"

// 方式三：硬编码（仅本地开发，不推荐）
url = "jdbc:postgresql://localhost:5432/mydb"
```

**提示**：建议在 `gradle.properties` 中统一管理：

```properties
# gradle.properties
db.url=jdbc:postgresql://localhost:5432/mydb
db.user=postgres
db.password=postgres
```

### 2.3 生成策略

| 配置项 | 说明 | 推荐值 |
|--------|------|--------|
| `database.name` | jOOQ Meta 使用的数据库方言 | `org.jooq.meta.postgres.PostgresDatabase` |
| `database.schema` | 生成的 schema 范围 | `"public"` 或你的业务 schema |
| `generate.isJavaTimeTypes` | 使用 `java.time` 类型 | `true` |
| `generate.isRecords` | 生成 `*Record` 类 | `true` |
| `generate.isPojos` | 生成 POJO 类 | `false`（使用 Record 即可） |
| `generate.isDaos` | 生成 DAO 类 | `true` |

**过滤表**（可选）：

```kotlin
database {
    // 只生成包含特定前缀的表
    includes = "USER_.*"
    // 排除某些表
    excludes = "FLYWAY.*"
}
```

### 2.4 输出配置

| 配置项 | 说明 | 推荐约定 |
|--------|------|---------|
| `target.packageName` | 生成类的包名 | `com.example.db`（业务根包 + `.db`） |
| `target.directory` | 输出目录 | `build/generated/jooq` |

**包名约定建议**：
- 代码：`com.example.domain`、`com.example.application`
- jOOQ 生成：`com.example.db`
- 分离清晰，避免混淆

### 2.5 任务依赖

```kotlin
tasks.named<nu.studer.jooq.GenerateJooqTask>("generateJooq") {
    dependsOn("flywayMigrate")
}
```

**原因**：jOOQ 代码生成依赖数据库 schema 已存在。Flyway 迁移负责创建/更新表结构，因此必须先执行 `flywayMigrate`，再执行 `generateJooq`。

**常见错误**：如果顺序错误，生成的代码可能与当前 schema 不一致，或生成失败（表不存在）。

---

## 三、版本管理

### 3.1 BOM 管理

cartisan-dependencies 通过 jOOQ BOM 统一管理版本：

```kotlin
// cartisan-dependencies/build.gradle.kts
dependencies {
    api(platform("org.jooq:jooq-bom:3.19.29"))
}
```

业务项目只需引入 BOM，无需显式指定 jOOQ 版本：

```kotlin
dependencies {
    api(platform(project(":cartisan-dependencies")))
    // jOOQ 版本由 BOM 管理
    implementation("org.jooq:jooq")
}
```

### 3.2 版本覆盖（可选）

如需自定义 jOOQ 版本（不推荐）：

```kotlin
dependencies {
    api(platform(project(":cartisan-dependencies")))

    // 强制使用特定版本
    implementation("org.jooq:jooq:3.20.0") {
        because("临时升级测试")
    }
}
```

**注意**：版本覆盖应谨慎，可能与框架其他部分不兼容。

---

## 四、常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 生成失败：`Table 'xxx' doesn't exist` | Flyway 迁移未执行或失败 | 1. 先运行 `./gradlew flywayMigrate`<br>2. 检查迁移脚本是否正确 |
| 生成后 IDEA 报 "Cannot find symbol" | IDEA 未识别生成目录为源码目录 | 1. 运行 `./gradlew cleanIdea idea`<br>2. 或手动标记为 Generated Sources Root |
| `generateJooq` 任务不存在 | 未应用 jOOQ 插件或配置错误 | 检查 `plugins { id("nu.studer.jooq") version "..." }` |
| 生成的包名不符合项目约定 | `target.packageName` 配置错误 | 修改为 `com.{公司}.{项目}.db` |
| 生成代码与数据库不一致 | 缓存问题或 schema 变更未重新生成 | 1. `./gradlew clean generateJooq`<br>2. 确认 Flyway 迁移已执行 |
| JDBC 连接失败 | 生成时使用的数据库配置错误 | 检查 `db.url`、`db.user`、`db.password` 是否正确 |

---

## 五、与 AI 协作

jOOQ 生成的 `Table`、`Field` 类是强类型的，非常适合 AI 辅助编码。AI 可以：

- 自动补全表名、字段名
- 理解字段类型（如 `USER.ID` 是 `Long` 类型）
- 辅助编写 DSL 查询（如 `dsl.selectFrom(USER).where(...)`）

建议在项目中配置 AI 上下文（如 CLAUDE.md）时说明：
- jOOQ 生成代码位于 `build/generated/jooq`
- 包名约定为 `com.example.db`
- 查询风格优先使用 DSL 而非原生 SQL

**参考**：更多 AI 协作规范，见 [AI协作开发SOP.md](../sop/AI协作开发SOP.md)。

---

## 参考资源

- jOOQ 官方文档：https://www.jooq.org/doc/latest/
- nu.studer.jooq 插件文档：https://github.com/etiennestuder/gradle-jooq-plugin
- cartisan-boot 设计文档：[cartisan-boot-设计文档.md](../cartisan-boot-设计文档.md)
