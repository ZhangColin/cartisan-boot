# Project Scaffolding Script 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建一个交互式 Bash 脚本，通过回答 3-4 个问题生成服务类/网关类/前端项目骨架。

**Architecture:** 单文件 `scripts/create-project.sh`，所有模板以 heredoc 嵌入，变量替换生成项目文件。脚本分三个生成器函数（generate_service / generate_gateway / generate_frontend），共享公共的部署脚本生成函数。

**Tech Stack:** Bash 5, heredoc, sed/variable substitution

**Spec:** `docs/superpowers/specs/2026-04-12-project-scaffolding-design.md`

---

## File Structure

| 文件 | 职责 |
|---|---|
| `scripts/create-project.sh` | 唯一需要创建的文件，包含所有逻辑和模板 |

### 脚本内部结构

| 区域 | 职责 |
|---|---|
| 配置区 | GROUP_ID, SERVER_IP, CARTISAN_VERSION 等常量 |
| 工具函数 | to_camel_case, to_snake_case, derive_values |
| 交互流程 | read 项目类型、项目名、端口、应用名 |
| generate_java_common() | 生成 Dockerfile, docker-compose.prod.yml, deploy.sh, publish.sh, .gitignore, .env.production.example（服务类和网关类共用） |
| generate_service() | 调用 generate_java_common，生成 pom.xml（无 security）、application 配置、logback-spring.xml、主类、测试类 |
| generate_gateway() | 调用 generate_java_common，生成 pom.xml（含 security + openapi）、application 配置（含 Sa-Token + openapi）、logback-spring.xml、主类、测试类 |
| generate_frontend() | 生成 package.json, Dockerfile(多阶段), docker-compose.prod.yml, deploy.sh, publish.sh, next.config.mjs, tsconfig, tailwind, postcss, eslint, layout, page, globals.css, utils.ts |
| main() | 交互流程 → derive_values → 调用对应 generator → 输出后续步骤提示 |

### 不在范围内

- 不自动执行 mvn install / pnpm install
- 不初始化 git
- 不预装 shadcn/ui 组件（用户按需 `npx shadcn@latest add xxx`）
- 不生成业务代码
- 不导入 cartisan-dependencies BOM
- 不生成 middleware.ts（用 next.config.mjs rewrites 代替）

---

### Task 1: 脚本骨架 — 配置区 + 工具函数 + 交互流程

**Files:**
- Create: `scripts/create-project.sh`

- [ ] **Step 1: 创建脚本文件**

创建 `scripts/create-project.sh`，包含：

1. **Shebang** `#!/bin/bash` + `set -e`
2. **配置区**：按 spec 定义所有常量（GROUP_ID, GROUP_ID_PATH, SERVER_IP, DEPLOY_BASE_DIR, CARTISAN_VERSION, SPRING_BOOT_VERSION, JAVA_BASE_IMAGE, NODE_BASE_IMAGE）。CARTISAN_VERSION 从框架根 pom.xml 的 `<version>` 读取（`grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/'`），如果读取失败用硬编码默认值
3. **工具函数**：
   - `to_camel_case(str)`: 将 snake_case/kebab-case 转大驼峰（hcy_payment → HcyPayment）
   - `to_package_name(str)`: 转纯小写无分隔符（hcy_payment → hcypayment）
   - `to_snake_case(str)`: 连字符转下划线
4. **交互流程**：按 spec 的 4 个问题（类型、项目名、端口、应用名），使用 `read -p` 和 `select` 实现交互。端口根据类型设默认值（服务/网关 8081，前端 3001）。应用名默认从项目名转驼峰。**前端额外询问** `BACKEND_SERVICE`（后端网关 Docker 服务名，默认 `aieducenter-admin`）用于 .env.production.example 中的 BACKEND_URL
5. **变量推导**：根据用户输入推导 artifactId, mainClass, dockerName, deployDir, jarName, packageName, packagePath
6. **目录检查**：检查目标目录是否已存在，存在则报错退出
7. **主分发**：根据类型调用 generate_service / generate_gateway / generate_frontend
8. **结尾提示**：根据类型输出后续步骤（cp .env.production.example, mvn install 或 pnpm install）

- [ ] **Step 2: 验证脚本可运行**

Run: `bash scripts/create-project.sh` 然后按 Ctrl+C 中断
Expected: 脚本启动，显示交互提示（类型选择），无语法错误

- [ ] **Step 3: Commit**

```bash
git add scripts/create-project.sh
git commit -m "feat: add create-project.sh script skeleton with config and interactive flow"
```

---

### Task 2: Java 公共生成器 — Dockerfile + docker-compose + deploy.sh + publish.sh + .gitignore + .env.production.example

**Files:**
- Modify: `scripts/create-project.sh`（添加 generate_java_common 函数）

- [ ] **Step 1: 实现 generate_java_common()**

在脚本中添加 `generate_java_common()` 函数，生成服务类和网关类共用的 6 个文件：

1. **Dockerfile**：按 spec 的 Java Dockerfile 模板，替换 jarName 和 port
2. **docker-compose.prod.yml**：按 spec 的 Java compose 模板，替换 dockerName 和 port
3. **deploy.sh**：按 spec 的通用 deploy.sh 逻辑（7 步），使用 `docker compose`（v2 插件语法，无连字符），包含配置区 + 检查 env + 检查产物 + down + build + up + 健康检查 + 输出结果
4. **publish.sh**：按 spec 的 Java publish.sh，sshpass + scp 上传 + 远程部署，使用 `docker compose` v2 语法
5. **.gitignore**：标准 Java .gitignore（target/, *.class, .idea/, *.iml, .env.production 等）
6. **.env.production.example**：按 spec 的 Java env 模板，替换 project_name。包含 SPRING_PROFILES_ACTIVE=prod

所有文件使用 heredoc + 变量替换写入。deploy.sh 和 publish.sh 写入后执行 `chmod +x`。

注意：所有 `docker compose` 命令使用 v2 语法（无连字符）。

- [ ] **Step 2: 语法验证**

Run: `bash -n scripts/create-project.sh`
Expected: 无语法错误

- [ ] **Step 3: Commit**

```bash
git add scripts/create-project.sh
git commit -m "feat: add generate_java_common for shared Java deployment files"
```

---

### Task 3: 服务类生成器 — pom.xml + application 配置 + 主类 + 测试类 + logback

**Files:**
- Modify: `scripts/create-project.sh`（添加 generate_service 函数）

- [ ] **Step 1: 实现 generate_service()**

添加 `generate_service()` 函数，先生成目录结构，再调用 `generate_java_common()`，然后生成服务类特有文件：

1. **pom.xml**：
   - parent: com.cartisan:cartisan-boot:${CARTISAN_VERSION}
   - groupId/artifactId/version/name
   - properties: pitest.junit5.plugin.version, postgresql.version
   - dependencies: spring-boot starters (web, jpa, redis, actuator), flyway-core, flyway-database-postgresql, postgresql, cartisan modules (core, web, data-jpa, openapi, test), spring-security-crypto, hutool-all, mapstruct + processor, lombok + lombok-mapstruct-binding, springdoc-openapi, druid
   - build plugins: compiler (--enable-preview), surefire (--enable-preview --add-opens), spring-boot-maven-plugin (version, mainClass, jvmArguments --enable-preview, repackage goal), pitest (targetClasses, 70%, --enable-preview)

2. **application.yml**：server.port, profiles.active, flyway, jpa ddl-auto:none, actuator health, cartisan.openapi.sign 配置

3. **application-local.yml**：localhost PG + Redis, show-sql: true

4. **application-prod.yml**：env 变量注入 PG/Redis, show-sql: false

5. **logback-spring.xml**：com.cartisan=DEBUG, com.aieducenter=INFO, console appender

6. **{MainClass}Application.java**：SpringBootApplication 主类，package 为 packageName

7. **{MainClass}ApplicationTest.java**：@SpringBootTest 测试类，验证 context loads

8. **db/migration/**：空目录（用 .gitkeep）

参考实现：`/Users/zhangcolin/workspace/hcy_payment/pom.xml` 和 `src/main/resources/` 下的配置文件。注意：服务类不包含 cartisan-security。

- [ ] **Step 2: 测试生成服务类项目**

Run: 在 /tmp 下运行 `echo -e "1\ntest_service\n8081\nTestService\n" | bash scripts/create-project.sh`
Expected: 在 /tmp 或当前目录生成 test_service/ 目录，包含所有预期文件

- [ ] **Step 3: 检查生成的文件完整性**

Run: `find test_service/ -type f | sort`
Expected: 列出所有 spec 中定义的服务类文件（pom.xml, Dockerfile, docker-compose.prod.yml, .env.production.example, deploy.sh, publish.sh, .gitignore, application*.yml, logback-spring.xml, MainClass, Test）

Run: 清理 `rm -rf test_service/`

- [ ] **Step 4: Commit**

```bash
git add scripts/create-project.sh
git commit -m "feat: add generate_service for service project scaffolding"
```

---

### Task 4: 网关类生成器 — 在服务类基础上添加 security 配置

**Files:**
- Modify: `scripts/create-project.sh`（添加 generate_gateway 函数）

- [ ] **Step 1: 实现 generate_gateway()**

添加 `generate_gateway()` 函数，**先调用 generate_java_common()**，然后生成网关类特有文件：

1. **pom.xml**：与服务类相同，但额外添加 `cartisan-security` 依赖

2. **application.yml**：与服务类相同的基础配置，额外包含：
   - Sa-Token 配置（token-name: Authorization, timeout: 604800, active-timeout: -1）
   - cartisan.security.interceptor 配置（includePaths: ["/**"], excludePaths: ["/error", "/actuator/**", "/auth/login"]）
   - cartisan.openapi.sign 配置（与 service 相同，作为客户端调下游）
   - flyway.table: {project_name}_flyway_schema_history
   - flyway.baseline-on-migrate: true
   - flyway.baseline-version: "0"

3. **.env.production.example**：在服务类 env 基础上无额外字段（Sa-Token 使用默认配置无需额外环境变量）

参考实现：`/Users/zhangcolin/workspace/aieducenter-admin/` 下的配置文件。

- [ ] **Step 2: 测试生成网关类项目**

Run: 在临时目录运行脚本生成网关类项目
Expected: pom.xml 包含 cartisan-security 和 cartisan-openapi；application.yml 包含 Sa-Token 和 security interceptor 配置

Run: 清理测试目录

- [ ] **Step 3: Commit**

```bash
git add scripts/create-project.sh
git commit -m "feat: add generate_gateway for gateway project scaffolding"
```

---

### Task 5: 前端生成器 — 配置文件 + Docker + 部署脚本

**Files:**
- Modify: `scripts/create-project.sh`（添加 generate_frontend 函数）

- [ ] **Step 5a: 项目配置文件**

添加 `generate_frontend()` 函数，先生成目录结构，然后生成配置文件：

1. **package.json**：name, version, scripts (dev/build/start/lint), dependencies (next 15, react 19, react-dom 19, zustand, lucide-react, next-themes, class-variance-authority, clsx, tailwind-merge), devDependencies (typescript, @types/react, @types/react-dom, @types/node, tailwindcss, postcss, autoprefixer, tailwindcss-animate, eslint, eslint-config-next)。不包含 @radix-ui（用户通过 `npx shadcn@latest add` 按需添加时自动安装）

2. **tsconfig.json**：target ES2022, moduleResolution bundler, paths @/* → ./src/*

3. **next.config.mjs**：reactStrictMode, output: 'standalone', rewrites: /api/* → ${BACKEND_URL}/api/*

4. **tailwind.config.ts**：darkMode 'class', content paths, 自定义主题色 (primary), plugins: [require('tailwindcss-animate')]

5. **postcss.config.mjs**：tailwindcss + autoprefixer

6. **.eslintrc.json**：next/core-web-vitals

7. **.gitignore**：前端 .gitignore（node_modules/, .next/, .env*.local, .env.production 等）

- [ ] **Step 5b: Docker 和部署文件**

继续在 generate_frontend() 中生成部署文件：

1. **Dockerfile**：三阶段多阶段构建（deps → builder → runner），按 spec 的前端 Dockerfile 模板，含 corepack enable + pnpm

2. **docker-compose.prod.yml**：按 spec 的前端 compose 模板，healthcheck 用 wget

3. **deploy.sh**：通用 deploy.sh 逻辑（7 步），使用 `docker compose` v2 语法，配置区适配前端

4. **publish.sh**：rsync 排除 node_modules/.next/.git/docs/.claude（不使用 --delete），sshpass 远程部署

5. **.env.production.example**：BACKEND_URL（使用 Task 1 中用户输入的 BACKEND_SERVICE）+ SERVER_PASSWORD

- [ ] **Step 5c: 源码文件**

继续在 generate_frontend() 中生成源码文件：

1. **src/app/layout.tsx**：根布局 HTML 结构，globals.css import

2. **src/app/page.tsx**：简单首页

3. **src/app/globals.css**：Tailwind directives (@tailwind base/components/utilities)

4. **src/lib/utils.ts**：cn() 函数（clsx + tailwind-merge）

5. **src/public/.gitkeep**：静态资源占位

参考实现：`/Users/zhangcolin/workspace/aieducenter-admin-web/`。

- [ ] **Step 5d: 测试生成前端项目**

Run: 在临时目录运行脚本生成前端项目
Expected: 生成完整的 Next.js 项目骨架，包含所有配置文件

- [ ] **Step 5e: 检查文件完整性**

Run: `find {test_dir}/ -type f | sort`
Expected: 列出所有 spec 中定义的前端文件

Run: 清理测试目录

- [ ] **Step 5f: Commit**

```bash
git add scripts/create-project.sh
git commit -m "feat: add generate_frontend for Next.js project scaffolding"
```

---

### Task 6: 集成测试 — 验证三种项目类型均可正确生成

- [ ] **Step 1: 测试服务类生成**

Run: 创建临时目录，用 echo 管道输入 `1\nhcy_payment\n8081\nHcyPayment` 运行脚本
验证：
- `ls -la hcy_payment/` — 目录和文件存在
- `grep 'cartisan-openapi' hcy_payment/pom.xml` — 有 openapi 依赖
- `grep 'cartisan-security' hcy_payment/pom.xml` — 不应存在
- `grep 'server.port' hcy_payment/src/main/resources/application.yml` — 端口为 8081
- `grep 'Sa-Token' hcy_payment/src/main/resources/application.yml` — 不应存在
- `cat hcy_payment/deploy.sh` — 可读，使用 `docker compose` 语法
- `cat hcy_payment/publish.sh` — 可读
- `cat hcy_payment/src/main/resources/logback-spring.xml` — 可读

清理

- [ ] **Step 2: 测试网关类生成**

Run: 创建临时目录，用 echo 管道输入 `2\ntest-admin\n8081\nTestAdmin` 运行脚本
验证：
- `grep 'cartisan-security' test-admin/pom.xml` — 有 security 依赖
- `grep 'cartisan-openapi' test-admin/pom.xml` — 有 openapi 依赖
- `grep 'sa-token' test-admin/src/main/resources/application.yml` — 有 Sa-Token 配置
- `grep 'interceptor' test-admin/src/main/resources/application.yml` — 有 interceptor 配置
- `grep 'baseline-on-migrate' test-admin/src/main/resources/application.yml` — 有 Flyway baseline
- `grep 'docker compose' test-admin/deploy.sh` — 使用 v2 语法

清理

- [ ] **Step 3: 测试前端类生成**

Run: 创建临时目录，用 echo 管道输入 `3\ntest-web\n3001\nTestWeb\naieducenter-admin` 运行脚本
验证：
- `cat test-web/package.json` — 有 next, react, zustand 依赖
- `cat test-web/Dockerfile` — 三阶段构建
- `cat test-web/next.config.mjs` — standalone + rewrites
- `cat test-web/tailwind.config.ts` — 有 tailwindcss-animate 插件
- `ls test-web/src/app/layout.tsx test-web/src/app/page.tsx` — 存在
- `ls test-web/src/public/.gitkeep` — 存在
- `grep BACKEND_URL test-web/.env.production.example` — 有后端服务地址

清理

- [ ] **Step 4: 测试目录重复检查**

Run: 在同一目录运行脚本两次（同项目名）
Expected: 第二次运行应报错退出，提示目录已存在

清理

- [ ] **Step 5: 最终 Commit**

```bash
git add scripts/create-project.sh
git commit -m "feat: complete project scaffolding script with all three project types"
```
