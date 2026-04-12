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

# Replace hyphens with underscores
# e.g., my-project -> my_project
to_snake_case() {
  local input="$1"
  echo "$input" | tr '-' '_'
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
read -p "请输入项目名 (例: hcy_payment): " project_name
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
dockerName=$(to_snake_case "$project_name")
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
SERVER_PASSWORD=\$(grep SERVER_PASSWORD .env.production | cut -d= -f2)

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

  cat << 'PUBLISH_EOF3' >> "$project_name/publish.sh"
# 3. 检查 sshpass
if ! command -v sshpass &>/dev/null; then
  error "sshpass 未安装"
  echo "请执行: brew install hudochenkov/sshpass/sshpass"
  exit 1
fi

# 4. 创建临时目录，只包含需要的文件
TMP_DIR=$(mktemp -d)
info "准备部署文件..."
cp "target/${jarName}" "$TMP_DIR/"
cp Dockerfile "$TMP_DIR/"
cp docker-compose.prod.yml "$TMP_DIR/"
cp deploy.sh "$TMP_DIR/"
cp .env.production "$TMP_DIR/"

# 5. rsync 到服务器
info "正在上传文件到服务器..."
sshpass -p "$SERVER_PASSWORD" rsync -avz --delete "$TMP_DIR/" "${SERVER_USER}@${SERVER_HOST}:${SERVER_PATH}/"

# 6. SSH 执行部署
info "正在远程部署..."
sshpass -p "$SERVER_PASSWORD" ssh "${SERVER_USER}@${SERVER_HOST}" "cd ${SERVER_PATH} && chmod +x deploy.sh && bash deploy.sh"

# 7. 清理临时目录
rm -rf "$TMP_DIR"
info "部署完成！临时文件已清理"
PUBLISH_EOF3

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

# 数据库
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${project_name}
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
EOF

  # 设置可执行权限
  chmod +x "$project_name/deploy.sh" "$project_name/publish.sh"
}

# ----------------------------------------------------------------------------
# 生成器函数（占位，后续任务填充）
# ----------------------------------------------------------------------------

generate_service() {
  echo "TODO: generate service project for $project_name"
}

generate_gateway() {
  echo "TODO: generate gateway project for $project_name"
}

generate_frontend() {
  echo "TODO: generate frontend project for $project_name"
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
    echo "  cp .env.production.example .env.production"
    echo "  # 编辑 .env.production"
    echo "  mvn install"
    ;;
  frontend)
    echo "  cd $project_name"
    echo "  cp .env.production.example .env.production"
    echo "  # 编辑 .env.production"
    echo "  pnpm install"
    ;;
esac

echo "========================================="
