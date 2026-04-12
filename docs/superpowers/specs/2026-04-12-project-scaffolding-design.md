# Cartisan 项目脚手架设计

## 目标

创建一个交互式 Bash 脚本 `scripts/create-project.sh`，通过回答少量问题即可生成完整的项目骨架，包含必要的源码结构、Docker 配置和一键部署脚本。

## 约束

- 单文件 Bash 脚本，所有模板以 heredoc 形式嵌入
- 放在 `cartisan-boot/scripts/` 目录
- 不需要额外的模板文件或依赖

## 脚本配置区（顶部集中管理）

```bash
# ========== 全局配置 ==========
GROUP_ID="com.aieducenter"
GROUP_ID_PATH="com/aieducenter"
SERVER_IP="43.140.211.9"
DEPLOY_BASE_DIR="/opt/hcy"
CARTISAN_VERSION="0.1.0-SNAPSHOT"  # 从 pom.xml 自动读取
JAVA_BASE_IMAGE="eclipse-temurin:21-jre-alpine"
NODE_BASE_IMAGE="node:20-alpine"
```

生成的 `publish.sh` 同样在顶部集中配置：
```bash
SERVER_IP="43.140.211.9"
DEPLOY_DIR="/opt/hcy/{project_name}"
SSHPASS_FILE=".env.production"  # 密码从此文件的 SERVER_PASSWORD 字段读取
```

## 交互流程

```
=== Cartisan 项目创建器 ===

请选择项目类型:
  1) 服务类 (Service)
  2) 网关类 (Gateway)
  3) 前端 (Frontend)
请输入项目名 (例: hcy_payment):
请输入端口号 [默认根据类型自动填写]:
请输入应用名称 [默认: 项目名转驼峰]:
```

**自动推导的值：**

| 值 | 推导规则 | 示例 |
|---|---|---|
| artifactId | 项目名 | hcy_payment |
| mainClass | 项目名转大驼峰 + Application | HcyPaymentApplication |
| dockerName | 项目名（连字符转下划线） | hcy_payment |
| deployDir | DEPLOY_BASE_DIR + / + dockerName | /opt/hcy/hcy_payment |
| jarName | artifactId + -1.0.0-SNAPSHOT.jar | hcy_payment-1.0.0-SNAPSHOT.jar |
| packageName | GROUP_ID + . + 项目名（去分隔符） | com.aieducenter.hcypayment |

端口号默认值：服务类 8081，网关类 8081，前端 3001。

## 三种项目类型的设计

### 服务类 (Service)

**cartisan 依赖**：cartisan-core + cartisan-web + cartisan-data-jpa + cartisan-openapi + cartisan-test

**生成的文件：**

```
{project_name}/
├── pom.xml                          # parent=cartisan-boot
├── Dockerfile                       # eclipse-temurin:21-jre-alpine
├── docker-compose.prod.yml          # webnet + 健康检查 + 日志轮转
├── .env.production.example          # DB/Redis/OpenAPI 配置模板
├── deploy.sh                        # 本地 Docker 部署
├── publish.sh                       # sshpass scp 上传 + 远程部署
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/aieducenter/{pkg}/
    │   │   └── {MainClass}Application.java
    │   └── resources/
    │       ├── application.yml      # 基础配置 + openapi 签名
    │       ├── application-local.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    └── test/
        └── java/com/aieducenter/{pkg}/
            └── {MainClass}ApplicationTest.java
```

**pom.xml 要点：**
- parent: com.cartisan:cartisan-boot:${CARTISAN_VERSION}
- 依赖: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-boot-starter-actuator
- cartisan: core, web, data-jpa, openapi, test
- 数据库: postgresql, flyway, druid
- 工具: hutool, mapstruct, springdoc-openapi
- build: spring-boot-maven-plugin, pitest (70%)

**application.yml 要点：**
- server.port: ${PORT}
- spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}
- cartisan.openapi.sign.enabled: true
- cartisan.openapi.sign.app-id: ${OPENAPI_APP_ID:}
- cartisan.openapi.sign.app-secret: ${OPENAPI_APP_SECRET:}
- spring.flyway enabled, locations: classpath:db/migration
- spring.jpa ddl-auto: none
- management.endpoints: health

**application-local.yml：**
- postgresql: localhost:5432/{project_name}
- redis: localhost:6379
- show-sql: true

**application-prod.yml：**
- 所有敏感配置通过环境变量注入
- show-sql: false

### 网关类 (Gateway)

**cartisan 依赖**：cartisan-core + cartisan-web + cartisan-data-jpa + cartisan-security + cartisan-openapi + cartisan-test

**生成的文件：** 结构与服务类相同

**与服务类的区别：**

1. **pom.xml** 额外引入 `cartisan-security`
2. **application.yml** 额外包含：
   - Sa-Token 配置（token-name, timeout, active-timeout）
   - cartisan.security.interceptor 配置（路径匹配规则）
   - cartisan.openapi 签名配置（作为客户端调用下游服务）
3. **.env.production.example** 额外包含 Sa-Token 和 OpenAPI 相关配置

### 前端 (Frontend)

**技术栈：** Next.js 15 + React 19 + shadcn/ui + Tailwind CSS + Zustand + TypeScript + pnpm

**生成的文件：**

```
{project_name}/
├── package.json
├── Dockerfile                       # 多阶段构建: node:20-alpine
├── docker-compose.prod.yml          # webnet + BACKEND_URL
├── .env.production.example          # BACKEND_URL
├── deploy.sh                        # 服务器端构建 + 部署
├── publish.sh                       # rsync 上传 + 远程部署
├── .gitignore
├── next.config.mjs                  # standalone output + API rewrite
├── tsconfig.json
├── tailwind.config.ts               # 自定义主题色
├── postcss.config.mjs
├── .eslintrc.json
└── src/
    ├── app/
    │   ├── layout.tsx               # 根布局
    │   ├── page.tsx                 # 首页
    │   └── globals.css              # Tailwind 基础样式
    ├── lib/
    │   └── utils.ts                 # cn() 工具函数
    └── middleware.ts                # 认证中间件
```

**package.json 要点：**
- next, react, react-dom
- tailwindcss, postcss, autoprefixer
- @radix-ui 相关 (shadcn/ui 基础)
- zustand (状态管理)
- lucide-react (图标)
- next-themes (暗色模式)
- class-variance-authority, clsx, tailwind-merge

**Dockerfile（多阶段构建）：**
- deps: 安装依赖
- builder: 构建 Next.js standalone output
- runner: 生产运行时，非 root 用户，仅复制必要文件

**deploy.sh（前端特殊）：**
- 服务器上执行 pnpm install + pnpm build（在 Docker 内）
- 不需要本地构建产物

**publish.sh（前端特殊）：**
- rsync 排除 node_modules, .next, .git, docs
- 只上传源码和 Docker 配置

## 部署脚本设计

### deploy.sh（通用逻辑）

```bash
# ========== 配置区 ==========
SERVICE_NAME="{docker_name}"
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.production"
HEALTH_CHECK_URL="http://localhost:{port}/{health_path}"
MAX_WAIT=30

# ========== 逻辑区 ==========
1. 检查 $ENV_FILE 存在
2. 检查构建产物存在（JAR / 前端源码）
3. docker compose -f $COMPOSE_FILE down
4. docker compose -f $COMPOSE_FILE build
5. docker compose -f $COMPOSE_FILE up -d
6. 等待健康检查通过（循环 curl，最多 MAX_WAIT 秒）
7. 输出部署结果
```

### publish.sh（Java 项目）

```bash
# ========== 配置区 ==========
SERVER_IP="43.140.211.9"
DEPLOY_DIR="/opt/hcy/{docker_name}"
SERVER_PASSWORD=$(grep SERVER_PASSWORD .env.production | cut -d= -f2)
JAR_FILE="target/{jarName}"
UPLOAD_FILES="Dockerfile docker-compose.prod.yml deploy.sh .env.production $JAR_FILE"

# ========== 逻辑区 ==========
1. mvn package -DskipTests
2. 检查 JAR 文件存在
3. sshpass -p $PASSWORD scp $UPLOAD_FILES root@$SERVER_IP:$DEPLOY_DIR/
4. sshpass -p $PASSWORD ssh root@$SERVER_IP "cd $DEPLOY_DIR && chmod +x deploy.sh && ./deploy.sh"
```

### publish.sh（前端项目）

```bash
# ========== 配置区 ==========
SERVER_IP="43.140.211.9"
DEPLOY_DIR="/opt/hcy/{docker_name}"
SERVER_PASSWORD=$(grep SERVER_PASSWORD .env.production | cut -d= -f2)
RSYNC_EXCLUDE="--exclude node_modules --exclude .next --exclude .git --exclude docs --exclude .claude"

# ========== 逻辑区 ==========
1. sshpass -p $PASSWORD rsync -avz --delete $RSYNC_EXCLUDE ./ root@$SERVER_IP:$DEPLOY_DIR/
2. sshpass -p $PASSWORD ssh root@$SERVER_IP "cd $DEPLOY_DIR && chmod +x deploy.sh && ./deploy.sh"
```

## .env.production.example 模板

### Java 项目

```env
# 数据库
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/{project_name}
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# OpenAPI 签名
OPENAPI_APP_ID=
OPENAPI_APP_SECRET=

# 服务器部署（publish.sh 使用）
SERVER_PASSWORD=
```

### 前端项目

```env
# 后端 API 地址
BACKEND_URL=http://{gateway_name}:8081

# 服务器部署（publish.sh 使用）
SERVER_PASSWORD=
```

## 生成后行为

1. 创建目录结构
2. 写入所有文件（heredoc + 变量替换）
3. 设置脚本执行权限（deploy.sh, publish.sh）
4. Java 项目：提示用户 `mvn install` 初始化
5. 前端项目：提示用户 `pnpm install` 初始化
6. 提示用户复制 `.env.production.example` 为 `.env.production` 并填写真实值

## 不做的事

- 不自动执行 mvn install / pnpm install（避免意外下载大量依赖）
- 不初始化 git（用户自己决定何时 init）
- 不预装 shadcn/ui 组件（用户按需添加）
- 不生成业务代码（Controller/Service/Repository 等）
