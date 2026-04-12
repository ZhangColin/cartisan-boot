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
