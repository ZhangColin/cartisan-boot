#!/bin/bash
set -e

# ============================================================================
# create-project.sh — AIEduCenter 项目脚手架脚本
# ============================================================================

# ----------------------------------------------------------------------------
# 配置区
# ----------------------------------------------------------------------------
GROUP_ID="com.aieducenter"
GROUP_ID_PATH="com/aieducenter"
SERVER_IP="43.140.211.9"
DEPLOY_BASE_DIR="/opt/hcy"
SPRING_BOOT_VERSION="3.4.1"
JAVA_BASE_IMAGE="eclipse-temurin:21-jre-alpine"
NODE_BASE_IMAGE="node:20-alpine"

# Auto-detect CARTISAN_VERSION from parent pom.xml
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
if [ -f "$REPO_ROOT/pom.xml" ]; then
  CARTISAN_VERSION=$(grep -m1 '<version>' "$REPO_ROOT/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' || true)
fi
CARTISAN_VERSION="${CARTISAN_VERSION:-0.1.0-SNAPSHOT}"

# ----------------------------------------------------------------------------
# 工具函数
# ----------------------------------------------------------------------------

# Convert snake_case/kebab-case to UpperCamelCase
# e.g., hcy_payment -> HcyPayment, my-gateway -> MyGateway
to_camel_case() {
  local input="$1"
  local result=""
  # Replace underscores and hyphens with spaces for word splitting
  local normalized
  normalized=$(echo "$input" | tr '_-' '  ')

  for word in $normalized; do
    if [ -n "$word" ]; then
      # Capitalize first letter, lowercase the rest
      local first_char
      first_char=$(echo "${word:0:1}" | tr '[:lower:]' '[:upper:]')
      local rest
      rest=$(echo "${word:1}" | tr '[:upper:]' '[:lower:]')
      result="${result}${first_char}${rest}"
    fi
  done

  echo "$result"
}

# Convert to lowercase with no separators
# e.g., hcy_payment -> hcypayment, MyGateway -> mygateway
to_package_name() {
  local input="$1"
  echo "$input" | tr '[:upper:]' '[:lower:]' | tr -d '_-'
}

# ----------------------------------------------------------------------------
# 交互流程
# ----------------------------------------------------------------------------

echo "========================================="
echo "  AIEduCenter 项目脚手架"
echo "========================================="
echo ""

# 项目类型选择
echo "请选择项目类型:"
echo "  1) 服务类 (Service)"
echo "  2) 网关类 (Gateway)"
echo "  3) 前端 (Frontend)"
read -p "请输入选项 [1-3]: " type_choice

case "$type_choice" in
  1) PROJECT_TYPE="service" ;;
  2) PROJECT_TYPE="gateway" ;;
  3) PROJECT_TYPE="frontend" ;;
  *)
    echo "错误: 无效选项 '$type_choice'"
    exit 1
    ;;
esac

# 项目名
read -p "请输入项目名 (例: aieducenter-admin): " project_name
if [ -z "$project_name" ]; then
  echo "错误: 项目名不能为空"
  exit 1
fi

# 默认端口号
case "$PROJECT_TYPE" in
  service|gateway) DEFAULT_PORT="8081" ;;
  frontend) DEFAULT_PORT="3001" ;;
esac

# 端口号
read -p "请输入端口号 [默认: $DEFAULT_PORT]: " port
port="${port:-$DEFAULT_PORT}"

# 应用名称（默认从项目名推导）
default_app_name=$(to_camel_case "$project_name")
read -p "请输入应用名称 [默认: $default_app_name]: " app_name
app_name="${app_name:-$default_app_name}"

# 前端特有：后端服务名
BACKEND_SERVICE=""
if [ "$PROJECT_TYPE" = "frontend" ]; then
  read -p "请输入后端服务名 [默认: aieducenter-admin]: " backend_service
  BACKEND_SERVICE="${backend_service:-aieducenter-admin}"
fi

# ----------------------------------------------------------------------------
# 变量推导
# ----------------------------------------------------------------------------
artifactId="$project_name"
mainClass="${app_name}Application"
dockerName=$(echo "$project_name" | tr '_' '-')
deployDir="${DEPLOY_BASE_DIR}/${dockerName}"
jarName="${artifactId}-1.0.0-SNAPSHOT.jar"
packageName="${GROUP_ID}.$(to_package_name "$app_name")"
packagePath="${GROUP_ID_PATH}/$(to_package_name "$app_name")"

# ----------------------------------------------------------------------------
# 打印确认信息
# ----------------------------------------------------------------------------
echo ""
echo "========================================="
echo "  项目配置确认"
echo "========================================="
echo "  类型:       $PROJECT_TYPE"
echo "  项目名:     $project_name"
echo "  应用名:     $app_name"
echo "  端口:       $port"
echo "  主类:       $mainClass"
echo "  Docker名:   $dockerName"
echo "  部署目录:   $deployDir"
echo "  包名:       $packageName"
echo "  包路径:     $packagePath"
if [ "$PROJECT_TYPE" = "frontend" ]; then
  echo "  后端服务:   $BACKEND_SERVICE"
fi
echo "========================================="
echo ""

# ----------------------------------------------------------------------------
# 目录检查
# ----------------------------------------------------------------------------
if [ -d "$project_name" ]; then
  echo "错误: 目录 '$project_name' 已存在，请选择其他项目名"
  exit 1
fi

# ----------------------------------------------------------------------------
# Java 公共生成器（Service / Gateway 共用）
# ----------------------------------------------------------------------------

generate_java_common() {
  mkdir -p "$project_name"

  # --- Dockerfile ---
  cat << EOF > "$project_name/Dockerfile"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata curl && \\
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \\
    echo "Asia/Shanghai" > /etc/timezone && \\
    apk del tzdata
COPY target/${jarName} app.jar
EXPOSE ${port}
ENTRYPOINT ["java", "--enable-preview", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
EOF

  # --- docker-compose.prod.yml ---
  cat << EOF > "$project_name/docker-compose.prod.yml"
services:
  app:
    build: .
    container_name: ${dockerName}
    restart: unless-stopped
    env_file:
      - .env.production
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    networks:
      - webnet
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:${port}/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 60s
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

networks:
  webnet:
    external: true
EOF

  # --- deploy.sh ---
  cat << 'DEPLOY_EOF' > "$project_name/deploy.sh"
#!/bin/bash
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 步骤 1：检查 .env.production
if [ ! -f .env.production ]; then
  error "找不到 .env.production 文件"
  exit 1
fi
info "环境配置文件检查通过"

# 步骤 2：检查 JAR 文件
DEPLOY_EOF

  cat << EOF >> "$project_name/deploy.sh"
if [ ! -f "target/${jarName}" ]; then
  error "找不到 JAR 文件: target/${jarName}"
  exit 1
fi
info "JAR 文件检查通过"

EOF

  cat << 'DEPLOY_EOF2' >> "$project_name/deploy.sh"
# 步骤 3：停止旧容器
docker compose -f docker-compose.prod.yml down 2>/dev/null || true
info "旧容器已停止"

# 步骤 4：构建镜像
info "正在构建 Docker 镜像..."
docker compose -f docker-compose.prod.yml build
info "镜像构建完成"

# 步骤 5：启动容器
info "正在启动容器..."
docker compose -f docker-compose.prod.yml up -d
info "容器已启动"

# 步骤 6：等待健康检查
info "正在等待健康检查..."
MAX_WAIT=30
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' $(docker compose -f docker-compose.prod.yml ps -q) 2>/dev/null || echo "unknown")
  if [ "$STATUS" = "healthy" ]; then
    break
  fi
  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

# 步骤 7：输出结果
if [ "$STATUS" = "healthy" ]; then
  info "部署成功！容器状态: healthy"
else
  warn "健康检查超时（${MAX_WAIT}s），当前状态: $STATUS"
  warn "请手动检查: docker compose -f docker-compose.prod.yml logs"
fi

# 步骤 8：清理悬空镜像
docker image prune -f >/dev/null 2>&1 || true
DEPLOY_EOF2

  # --- publish.sh ---
  cat << 'PUBLISH_HEAD' > "$project_name/publish.sh"
#!/bin/bash
set -e

# ========== 配置区 ==========
SERVER_USER="root"
SERVER_HOST="43.140.211.9"
PUBLISH_HEAD

  # SERVER_PATH needs variable substitution, SERVER_PASSWORD needs escaping
  cat << EOF >> "$project_name/publish.sh"
SERVER_PATH="${deployDir}"
SERVER_PASSWORD="Hcy@20260327"

EOF

  cat << 'PUBLISH_EOF2' >> "$project_name/publish.sh"
# ========== 逻辑区 ==========

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

SSH_CMD="sshpass -p $SERVER_PASSWORD ssh -o StrictHostKeyChecking=no"
RSYNC_CMD="sshpass -p $SERVER_PASSWORD rsync"

# 1. 本地构建
info "正在构建项目..."
mvn package -DskipTests -q

# 2. 检查 JAR 文件
PUBLISH_EOF2

  cat << EOF >> "$project_name/publish.sh"
if [ ! -f "target/${jarName}" ]; then
  error "构建失败：找不到 target/${jarName}"
  exit 1
fi
info "构建完成"

EOF

  cat << 'PUBLISH_EOF3A' >> "$project_name/publish.sh"
# 3. 检查 sshpass
if ! command -v sshpass &>/dev/null; then
  error "sshpass 未安装"
  echo "请执行: brew install hudochenkov/sshpass/sshpass"
  exit 1
fi

# 4. 创建临时目录，只包含需要的文件
TMP_DIR=$(mktemp -d)
info "准备部署文件..."
PUBLISH_EOF3A

  # JAR file copy needs variable substitution
  cat << EOF >> "$project_name/publish.sh"
mkdir -p "\$TMP_DIR/target"
cp "target/${jarName}" "\$TMP_DIR/target/"
EOF

  cat << 'PUBLISH_EOF3B' >> "$project_name/publish.sh"
cp Dockerfile "$TMP_DIR/"
cp docker-compose.prod.yml "$TMP_DIR/"
cp deploy.sh "$TMP_DIR/"
cp .env.production "$TMP_DIR/"

# 5. rsync 到服务器
info "正在上传文件到服务器..."
$SSH_CMD ${SERVER_USER}@${SERVER_HOST} "mkdir -p ${SERVER_PATH}/target"
$RSYNC_CMD -avz -e "ssh -o StrictHostKeyChecking=no" "$TMP_DIR/" ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/

# 6. SSH 执行部署
info "正在远程部署..."
$SSH_CMD ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && chmod +x deploy.sh && bash deploy.sh"

# 7. 清理临时目录
rm -rf "$TMP_DIR"
info "部署完成！临时文件已清理"
PUBLISH_EOF3B

  # --- .gitignore ---
  cat << 'EOF' > "$project_name/.gitignore"
target/
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Environment
.env.production
.env*.local
EOF

  # --- .env.production.example ---
  cat << EOF > "$project_name/.env.production.example"
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# 数据库连接（通过 webnet 网络直连 PG 容器）
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-server:5432/aieducenter
SPRING_DATASOURCE_USERNAME=hcy_pgsql
SPRING_DATASOURCE_PASSWORD=hcy_admin_pgsql

# Redis（通过 webnet 网络直连已有 Redis 容器）
REDIS_HOST=redis-cache
REDIS_PORT=6379

# OpenAPI 签名
OPENAPI_APP_ID=
OPENAPI_APP_SECRET=

# 服务器部署（publish.sh 使用）
SERVER_PASSWORD=Hcy@20260327
EOF

  # 同时生成 .env.production（填入实际值）
  cp "$project_name/.env.production.example" "$project_name/.env.production"

  # --- CLAUDE.md ---
  cat << EOF > "$project_name/CLAUDE.md"
# ${project_name}

${PROJECT_TYPE} 服务，基于 cartisan-boot 框架。

## 技术栈

- Java 21 / Spring Boot 3.4.x / Maven
- 持久化：Spring Data JPA + PostgreSQL / Redis
- 测试：JUnit 5 + AssertJ + Mockito

## 架构约束

- DDD 分层架构：controller / application / domain / infrastructure
- 构造函数注入，禁止 @Autowired 字段注入
- 金额使用 BigDecimal，禁止浮点数

## 编码规范

- DTO 使用 Java Record，构造函数校验不变量
- 测试使用 AssertJ，测试命名：shouldX 或 shouldX_whenY
- 主类：${packageName}.${mainClass}

## 常用命令

- 编译：\`mvn compile\`
- 单元测试：\`mvn test\`
- 打包：\`mvn package -DskipTests\`
- 变异测试：\`mvn org.pitest:pitest-maven:mutationCoverage\`

## 开发流程

使用 Superpowers 技能驱动开发，按需求规模分层：

- **大需求**：先充分讨论，产出需求设计文档（含 Epic 拆解），再逐个 Epic 推进
- **Epic / 中需求**：讨论后产出 Backlog 文档（含 Feature 拆解），再逐个 Feature 推进
- **Feature / 小需求 / Bug**：直接用 Superpowers 技能（brainstorming -> writing-plans -> TDD -> verification）
EOF

  # --- .claude/settings.local.json ---
  mkdir -p "$project_name/.claude"
  cat << 'SETTINGS_EOF' > "$project_name/.claude/settings.local.json"
{
  "permissions": {
    "allow": [
      "Bash",
      "Bash(git *)",
      "Bash(ls *)",
      "Bash(cd *)",
      "Bash(mkdir *)",
      "Bash(cat *)",
      "Bash(find *)",
      "Bash(grep *)",
      "WebSearch",
      "Skill(update-config)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force *)"
    ]
  }
}
SETTINGS_EOF

  # 设置可执行权限
  chmod +x "$project_name/deploy.sh" "$project_name/publish.sh"
}

# ----------------------------------------------------------------------------
# 生成器函数（占位，后续任务填充）
# ----------------------------------------------------------------------------

generate_service() {
  generate_java_common

  # --- 目录结构 ---
  mkdir -p "$project_name/src/main/java/$packagePath"
  mkdir -p "$project_name/src/main/resources/db/migration"
  mkdir -p "$project_name/src/test/java/$packagePath"

  # --- pom.xml ---
  cat << EOF > "$project_name/pom.xml"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>${CARTISAN_VERSION}</version>
    </parent>

    <groupId>${GROUP_ID}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <postgresql.version>42.7.4</postgresql.version>
        <pitest.junit5.plugin.version>1.2.3</pitest.junit5.plugin.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>10.18.0</version>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>\${postgresql.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Cartisan Modules -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-web</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-data-jpa</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-openapi</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok-mapstruct-binding</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-test</artifactId>
            <version>${CARTISAN_VERSION}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <compilerArgs>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>--enable-preview --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${SPRING_BOOT_VERSION}</version>
                <configuration>
                    <mainClass>${packageName}.${mainClass}</mainClass>
                    <jvmArguments>--enable-preview</jvmArguments>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                        <version>\${pitest.junit5.plugin.version}</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <targetClasses>
                        <param>${packageName}.*</param>
                    </targetClasses>
                    <mutators>DEFAULTS</mutators>
                    <mutationThreshold>70</mutationThreshold>
                    <jvmArgs>
                        <arg>--enable-preview</arg>
                    </jvmArgs>
                    <threads>2</threads>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

  # --- application.yml ---
  cat << EOF > "$project_name/src/main/resources/application.yml"
server:
  port: ${port}

spring:
  application:
    name: ${project_name}
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:local}
  flyway:
    enabled: true
    table: ${project_name}_flyway_schema_history
    baseline-on-migrate: true
    baseline-version: "0"
    locations: classpath:db/migration
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false

# OpenAPI 签名配置
cartisan:
  openapi:
    sign:
      enabled: true
      app-id: \${OPENAPI_APP_ID:}
      app-secret: \${OPENAPI_APP_SECRET:}
      apikey-service-url: https://openapi.aieducenter.com/api/v1/api-keys/by-appId

management:
  endpoints:
    web:
      exposure:
        include: health
      base-path: /actuator
  endpoint:
    health:
      show-details: never
EOF

  # --- application-local.yml ---
  cat << EOF > "$project_name/src/main/resources/application-local.yml"
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aieducenter
    username: aiedu
    password: dev123
  data:
    redis:
      host: localhost
      port: 6379

spring.jpa.show-sql: true
EOF

  # --- application-prod.yml ---
  cat << EOF > "$project_name/src/main/resources/application-prod.yml"
spring:
  datasource:
    url: \${SPRING_DATASOURCE_URL}
    username: \${SPRING_DATASOURCE_USERNAME}
    password: \${SPRING_DATASOURCE_PASSWORD}
  data:
    redis:
      host: \${REDIS_HOST}
      port: \${REDIS_PORT:6379}

spring.jpa.show-sql: false
logging.level.root: INFO
EOF

  # --- logback-spring.xml ---
  cat << 'EOF' > "$project_name/src/main/resources/logback-spring.xml"
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <logger name="com.cartisan" level="DEBUG"/>
    <logger name="com.aieducenter" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
EOF

  # --- MainApplication.java ---
  cat << EOF > "$project_name/src/main/java/$packagePath/${mainClass}.java"
package ${packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${mainClass} {
    public static void main(String[] args) {
        SpringApplication.run(${mainClass}.class, args);
    }
}
EOF

  # --- ApplicationTest.java ---
  cat << EOF > "$project_name/src/test/java/$packagePath/${mainClass}Test.java"
package ${packageName};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ${mainClass}Test {
    @Test
    void contextLoads() {
    }
}
EOF

  # --- db/migration/.gitkeep ---
  touch "$project_name/src/main/resources/db/migration/.gitkeep"
}

generate_gateway() {
  generate_java_common

  # --- 目录结构 ---
  mkdir -p "$project_name/src/main/java/$packagePath"
  mkdir -p "$project_name/src/main/resources/db/migration"
  mkdir -p "$project_name/src/test/java/$packagePath"

  # --- pom.xml ---
  cat << EOF > "$project_name/pom.xml"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cartisan</groupId>
        <artifactId>cartisan-boot</artifactId>
        <version>${CARTISAN_VERSION}</version>
    </parent>

    <groupId>${GROUP_ID}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <postgresql.version>42.7.4</postgresql.version>
        <pitest.junit5.plugin.version>1.2.3</pitest.junit5.plugin.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>10.18.0</version>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>\${postgresql.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Cartisan Modules -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-core</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-web</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-data-jpa</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-openapi</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-security</artifactId>
            <version>${CARTISAN_VERSION}</version>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok-mapstruct-binding</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>com.cartisan</groupId>
            <artifactId>cartisan-test</artifactId>
            <version>${CARTISAN_VERSION}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <compilerArgs>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>--enable-preview --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${SPRING_BOOT_VERSION}</version>
                <configuration>
                    <mainClass>${packageName}.${mainClass}</mainClass>
                    <jvmArguments>--enable-preview</jvmArguments>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                        <version>\${pitest.junit5.plugin.version}</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <targetClasses>
                        <param>${packageName}.*</param>
                    </targetClasses>
                    <mutators>DEFAULTS</mutators>
                    <mutationThreshold>70</mutationThreshold>
                    <jvmArgs>
                        <arg>--enable-preview</arg>
                    </jvmArgs>
                    <threads>2</threads>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

  # --- application.yml ---
  cat << EOF > "$project_name/src/main/resources/application.yml"
server:
  port: ${port}

spring:
  application:
    name: ${project_name}
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:local}
  flyway:
    enabled: true
    table: ${project_name}_flyway_schema_history
    baseline-on-migrate: true
    baseline-version: "0"
    locations: classpath:db/migration
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false

# Security 拦截配置
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/**"
      exclude-path-patterns:
        - "/error"
        - "/actuator/**"
        - "/auth/login"
        - "/auth/captcha"
        - "/health"
        - "/swagger-ui/**"
        - "/api-docs/**"
  openapi:
    sign:
      enabled: true
      app-id: \${OPENAPI_APP_ID:}
      app-secret: \${OPENAPI_APP_SECRET:}
      apikey-service-url: https://openapi.aieducenter.com/api/v1/api-keys/by-appId

# Sa-Token 配置
sa-token:
  token-name: Authorization
  timeout: 604800
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid

management:
  endpoints:
    web:
      exposure:
        include: health
      base-path: /actuator
  endpoint:
    health:
      show-details: never
EOF

  # --- application-local.yml ---
  cat << EOF > "$project_name/src/main/resources/application-local.yml"
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aieducenter
    username: aiedu
    password: dev123
  data:
    redis:
      host: localhost
      port: 6379

spring.jpa.show-sql: true
EOF

  # --- application-prod.yml ---
  cat << EOF > "$project_name/src/main/resources/application-prod.yml"
spring:
  datasource:
    url: \${SPRING_DATASOURCE_URL}
    username: \${SPRING_DATASOURCE_USERNAME}
    password: \${SPRING_DATASOURCE_PASSWORD}
  data:
    redis:
      host: \${REDIS_HOST}
      port: \${REDIS_PORT:6379}

spring.jpa.show-sql: false
logging.level.root: INFO
EOF

  # --- logback-spring.xml ---
  cat << 'EOF' > "$project_name/src/main/resources/logback-spring.xml"
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <logger name="com.cartisan" level="DEBUG"/>
    <logger name="com.aieducenter" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
EOF

  # --- MainApplication.java ---
  cat << EOF > "$project_name/src/main/java/$packagePath/${mainClass}.java"
package ${packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${mainClass} {
    public static void main(String[] args) {
        SpringApplication.run(${mainClass}.class, args);
    }
}
EOF

  # --- ApplicationTest.java ---
  cat << EOF > "$project_name/src/test/java/$packagePath/${mainClass}Test.java"
package ${packageName};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ${mainClass}Test {
    @Test
    void contextLoads() {
    }
}
EOF

  # --- db/migration/.gitkeep ---
  touch "$project_name/src/main/resources/db/migration/.gitkeep"
}

generate_frontend() {
  # --- 目录结构 ---
  mkdir -p "$project_name/src/app"
  mkdir -p "$project_name/src/lib"
  mkdir -p "$project_name/src/public"

  # --- package.json ---
  cat << EOF > "$project_name/package.json"
{
  "name": "${project_name}",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev -p ${port}",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "typecheck": "tsc --noEmit"
  },
  "dependencies": {
    "lucide-react": "^0.577.0",
    "next": "^15.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "zustand": "^5.0.12",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.0",
    "class-variance-authority": "^0.7.1",
    "next-themes": "^0.4.6"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "autoprefixer": "^10.4.27",
    "eslint": "^10.0.3",
    "eslint-config-next": "^16.1.6",
    "postcss": "^8.4.47",
    "tailwindcss": "3.4.19",
    "tailwindcss-animate": "^1.0.7",
    "typescript": "^5.0.0"
  }
}
EOF

  # --- next.config.mjs ---
  cat << 'EOF' > "$project_name/next.config.mjs"
/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  rewrites: async () => {
    const backendUrl = process.env.BACKEND_URL || 'http://localhost:8081'
    return [
      {
        source: '/api/:path*',
        destination: `${backendUrl}/api/:path*`,
      },
    ]
  },
}

export default nextConfig
EOF

  # --- tsconfig.json ---
  cat << 'EOF' > "$project_name/tsconfig.json"
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2023", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "allowJs": true,
    "strict": true,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,
    "forceConsistentCasingInFileNames": true,
    "noEmit": true,
    "incremental": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "plugins": [{ "name": "next" }],
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
EOF

  # --- tailwind.config.ts ---
  cat << 'EOF' > "$project_name/tailwind.config.ts"
import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: [
    './src/**/*.{ts,tsx}',
  ],
  theme: {
    container: {
      center: true,
      padding: '2rem',
      screens: {
        '2xl': '1400px',
      },
    },
    extend: {
      colors: {
        primary: '#308ce8',
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}

export default config
EOF

  # --- postcss.config.mjs ---
  cat << 'EOF' > "$project_name/postcss.config.mjs"
/** @type {import('postcss-load-config').Config} */
const config = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}

export default config
EOF

  # --- .eslintrc.json ---
  cat << 'EOF' > "$project_name/.eslintrc.json"
{
  "extends": "next/core-web-vitals"
}
EOF

  # --- Dockerfile (3-stage multi-stage build) ---
  cat << EOF > "$project_name/Dockerfile"
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml* ./
RUN corepack enable && pnpm install --frozen-lockfile || pnpm install

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN corepack enable && pnpm build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
RUN addgroup --system --gid 1001 nodejs && \\
    adduser --system --uid 1001 nextjs
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
USER nextjs
EXPOSE ${port}
ENV PORT=${port}
CMD ["node", "server.js"]
EOF

  # --- docker-compose.prod.yml ---
  cat << EOF > "$project_name/docker-compose.prod.yml"
services:
  app:
    build: .
    container_name: ${dockerName}
    restart: unless-stopped
    ports:
      - "${port}:${port}"
    env_file:
      - .env.production
    networks:
      - webnet
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:${port}/"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

networks:
  webnet:
    external: true
EOF

  # --- deploy.sh ---
  cat << 'DEPLOY_EOF' > "$project_name/deploy.sh"
#!/bin/bash
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 步骤 1：检查 .env.production
if [ ! -f .env.production ]; then
  error "找不到 .env.production 文件"
  exit 1
fi
info "环境配置文件检查通过"

# 步骤 2：停止旧容器
docker compose -f docker-compose.prod.yml down 2>/dev/null || true
info "旧容器已停止"

# 步骤 3：构建镜像
info "正在构建 Docker 镜像..."
docker compose -f docker-compose.prod.yml build
info "镜像构建完成"

# 步骤 4：启动容器
info "正在启动容器..."
docker compose -f docker-compose.prod.yml up -d
info "容器已启动"

# 步骤 5：等待健康检查
info "正在等待健康检查..."
MAX_WAIT=30
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' $(docker compose -f docker-compose.prod.yml ps -q) 2>/dev/null || echo "unknown")
  if [ "$STATUS" = "healthy" ]; then
    break
  fi
  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

# 步骤 6：输出结果
if [ "$STATUS" = "healthy" ]; then
  info "部署成功！容器状态: healthy"
else
  warn "健康检查超时（${MAX_WAIT}s），当前状态: $STATUS"
  warn "请手动检查: docker compose -f docker-compose.prod.yml logs"
fi

# 步骤 7：清理悬空镜像
docker image prune -f >/dev/null 2>&1 || true
DEPLOY_EOF

  # --- publish.sh ---
  cat << 'PUBLISH_HEAD' > "$project_name/publish.sh"
#!/bin/bash
set -e

# ========== 配置区 ==========
SERVER_USER="root"
SERVER_HOST="43.140.211.9"
PUBLISH_HEAD

  cat << EOF >> "$project_name/publish.sh"
SERVER_PATH="${deployDir}"
SERVER_PASSWORD="Hcy@20260327"

EOF

  cat << 'PUBLISH_BODY' >> "$project_name/publish.sh"
# ========== 逻辑区 ==========

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

SSH_CMD="sshpass -p $SERVER_PASSWORD ssh -o StrictHostKeyChecking=no"
RSYNC_CMD="sshpass -p $SERVER_PASSWORD rsync"

# 1. 检查 sshpass
if ! command -v sshpass &>/dev/null; then
  error "sshpass 未安装"
  echo "请执行: brew install hudochenkov/sshpass/sshpass"
  exit 1
fi

# 2. rsync 到服务器
info "正在上传文件到服务器..."
$RSYNC_CMD -avz -e "ssh -o StrictHostKeyChecking=no" \
  --exclude node_modules \
  --exclude .next \
  --exclude .git \
  --exclude docs \
  --exclude .claude \
  ./ ${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/

# 3. SSH 执行部署
info "正在远程部署..."
$SSH_CMD ${SERVER_USER}@${SERVER_HOST} "cd ${SERVER_PATH} && chmod +x deploy.sh && bash deploy.sh"

info "部署完成！"
PUBLISH_BODY

  # --- .env.production.example ---
  cat << EOF > "$project_name/.env.production.example"
# 后端 API 地址
BACKEND_URL=http://${BACKEND_SERVICE}:8081

# 服务器部署（publish.sh 使用）
SERVER_PASSWORD=Hcy@20260327
EOF

  # 同时生成 .env.production（填入实际值）
  cp "$project_name/.env.production.example" "$project_name/.env.production"

  # --- .gitignore ---
  cat << 'EOF' > "$project_name/.gitignore"
node_modules/
.next/
out/
.env.production
.env*.local

# IDE
.idea/
.vscode/
*.swp

# OS
.DS_Store
Thumbs.db
EOF

  # --- src/app/layout.tsx ---
  cat << 'EOF' > "$project_name/src/app/layout.tsx"
import './globals.css'

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  )
}
EOF

  # --- src/app/page.tsx ---
  cat << 'EOF' > "$project_name/src/app/page.tsx"
export default function Home() {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <h1 className="text-4xl font-bold">Welcome</h1>
    </main>
  )
}
EOF

  # --- src/app/globals.css ---
  cat << 'EOF' > "$project_name/src/app/globals.css"
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 0 0% 3.9%;
    --card: 0 0% 100%;
    --card-foreground: 0 0% 3.9%;
    --popover: 0 0% 100%;
    --popover-foreground: 0 0% 3.9%;
    --primary: 0 0% 9%;
    --primary-foreground: 0 0% 98%;
    --secondary: 0 0% 96.1%;
    --secondary-foreground: 0 0% 9%;
    --muted: 0 0% 96.1%;
    --muted-foreground: 0 0% 45.1%;
    --accent: 0 0% 96.1%;
    --accent-foreground: 0 0% 9%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 0 0% 98%;
    --border: 0 0% 89.8%;
    --input: 0 0% 89.8%;
    --ring: 0 0% 3.9%;
    --radius: 0.5rem;
  }

  .dark {
    --background: 0 0% 3.9%;
    --foreground: 0 0% 98%;
    --card: 0 0% 3.9%;
    --card-foreground: 0 0% 98%;
    --popover: 0 0% 3.9%;
    --popover-foreground: 0 0% 98%;
    --primary: 0 0% 98%;
    --primary-foreground: 0 0% 9%;
    --secondary: 0 0% 14.9%;
    --secondary-foreground: 0 0% 98%;
    --muted: 0 0% 14.9%;
    --muted-foreground: 0 0% 63.9%;
    --accent: 0 0% 14.9%;
    --accent-foreground: 0 0% 98%;
    --destructive: 0 62.8% 30.6%;
    --destructive-foreground: 0 0% 98%;
    --border: 0 0% 14.9%;
    --input: 0 0% 14.9%;
    --ring: 0 0% 83.1%;
  }
}

@layer base {
  * {
    border-color: hsl(var(--border));
  }
  body {
    background-color: hsl(var(--background));
    color: hsl(var(--foreground));
  }
}
EOF

  # --- src/lib/utils.ts ---
  cat << 'EOF' > "$project_name/src/lib/utils.ts"
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
EOF

  # --- src/public/.gitkeep ---
  touch "$project_name/src/public/.gitkeep"

  # --- CLAUDE.md ---
  cat << EOF > "$project_name/CLAUDE.md"
# ${project_name}

前端项目，基于 Next.js。

## 技术栈

- Next.js 15 / React 19 / TypeScript
- Tailwind CSS / Zustand
- pnpm 包管理

## 编码规范

- 函数组件 + hooks，禁止 class 组件
- TypeScript strict 模式
- 路径别名：\`@/*\` 映射 \`./src/*\`
- 端口：${port}

## 常用命令

- 开发：\`pnpm dev\`
- 构建：\`pnpm build\`
- 代码检查：\`pnpm lint\`
- 类型检查：\`pnpm typecheck\`

## 开发流程

使用 Superpowers 技能驱动开发，按需求规模分层：

- **大需求**：先充分讨论，产出需求设计文档（含 Epic 拆解），再逐个 Epic 推进
- **Epic / 中需求**：讨论后产出 Backlog 文档（含 Feature 拆解），再逐个 Feature 推进
- **Feature / 小需求 / Bug**：直接用 Superpowers 技能（brainstorming -> writing-plans -> TDD -> verification）
EOF

  # --- .claude/settings.local.json ---
  mkdir -p "$project_name/.claude"
  cat << 'SETTINGS_EOF' > "$project_name/.claude/settings.local.json"
{
  "permissions": {
    "allow": [
      "Bash",
      "Bash(git *)",
      "Bash(ls *)",
      "Bash(cd *)",
      "Bash(mkdir *)",
      "Bash(cat *)",
      "Bash(find *)",
      "Bash(grep *)",
      "WebSearch",
      "Skill(update-config)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(git push --force *)"
    ]
  }
}
SETTINGS_EOF

  # 设置可执行权限
  chmod +x "$project_name/deploy.sh" "$project_name/publish.sh"
}

# ----------------------------------------------------------------------------
# 主分发
# ----------------------------------------------------------------------------
case "$PROJECT_TYPE" in
  service)  generate_service ;;
  gateway)  generate_gateway ;;
  frontend) generate_frontend ;;
esac

# ----------------------------------------------------------------------------
# 后续步骤提示
# ----------------------------------------------------------------------------
echo ""
echo "========================================="
echo "  下一步"
echo "========================================="

case "$PROJECT_TYPE" in
  service|gateway)
    echo "  cd $project_name"
    echo "  # 编辑 .env.production 中的 OPENAPI_APP_ID / SERVER_PASSWORD"
    echo "  mvn install"
    ;;
  frontend)
    echo "  cd $project_name"
    echo "  # 编辑 .env.production 中的 SERVER_PASSWORD"
    echo "  pnpm install"
    ;;
esac

echo "========================================="
