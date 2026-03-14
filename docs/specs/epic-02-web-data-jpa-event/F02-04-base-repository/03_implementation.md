# Feature: F02-04 BaseRepository — 实施计划

## 目标复述

创建 `cartisan-data-jpa` 模块，定义 `BaseRepository<T extends AggregateRoot<?>, ID extends Serializable>` 接口。通过泛型约束在编译期强制"只有聚合根才能拥有 Repository"，继承 Spring Data JPA 的 `JpaRepository` 和 `JpaSpecificationExecutor` 提供标准 CRUD 能力。

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `settings.gradle.kts` | 添加 `include(":cartisan-data-jpa")` |
| 新增 | `cartisan-data-jpa/build.gradle.kts` | 模块构建配置，依赖 cartisan-core 和 spring-data-jpa |
| 新增 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/package-info.java` | 模块包说明 |
| 新增 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/package-info.java` | repository 包说明 |
| 新增 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/BaseRepository.java` | 核心接口 |

## 核心流程（伪代码）

```
1. 修改 settings.gradle.kts
   └─> include(":cartisan-data-jpa")

2. 创建 cartisan-data-jpa/build.gradle.kts
   ├─> 依赖 api(project(":cartisan-core"))
   ├─> 依赖 api("org.springframework.boot:spring-boot-starter-data-jpa")
   └─> Java 21 配置

3. 创建目录结构
   └─> com.cartisan.data.jpa.repository

4. 创建 BaseRepository.java
   ├─> 添加 @NoRepositoryBean 注解
   ├─> 声明泛型 <T extends AggregateRoot<?>, ID extends Serializable>
   ├─> 继承 JpaRepository<T, ID>, JpaSpecificationExecutor<T>
   └─> 添加 JavaDoc（含示例）

5. 创建 package-info.java 文件（模块和 repository 包各一个）

6. 编译验证
   └─> ./gradlew :cartisan-data-jpa:compileJava
```

## 原子任务清单

### Step 1: 修改 settings.gradle.kts

- **文件**: `settings.gradle.kts`
- **内容**: 追加 `include(":cartisan-data-jpa")`
- **验证**: Gradle 同步成功，识别新模块

### Step 2: 创建 build.gradle.kts

- **文件**: `cartisan-data-jpa/build.gradle.kts`
- **内容**:
  - Java 21 配置
  - `api(project(":cartisan-core"))`
  - `api("org.springframework.boot:spring-boot-starter-data-jpa")`
- **验证**: 配置语法正确

### Step 3: 创建目录结构

- **目录**: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/`
- **验证**: 目录创建成功

### Step 4: 创建 package-info.java（模块）

- **文件**: `com/cartisan/data/jpa/package-info.java`
- **内容**: 模块级别说明
- **验证**: 编译通过

### Step 5: 创建 package-info.java（repository）

- **文件**: `com/cartisan/data/jpa/repository/package-info.java`
- **内容**: repository 包职责说明
- **验证**: 编译通过

### Step 6: 创建 BaseRepository.java

- **文件**: `com/cartisan/data/jpa/repository/BaseRepository.java`
- **内容**:
  - `@NoRepositoryBean` 注解
  - 泛型声明：`<T extends AggregateRoot<?>, ID extends Serializable>`
  - 继承：`extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>`
  - JavaDoc（含示例）
- **验证**: 编译通过

### Step 7: 编译验证

- **命令**: `./gradlew :cartisan-data-jpa:compileJava`
- **预期**: 编译成功，无错误
