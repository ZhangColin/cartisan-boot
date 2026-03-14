#!/bin/bash
# PIT 变异测试执行脚本
# Phase 5 审查门禁：变异杀死率需 ≥ 70%

set -e

echo "🧪 Running PIT mutation testing..."

# 检查是否有指定模块
if [ -z "$1" ]; then
    echo "Usage: ./scripts/run-pitest.sh <module-name>"
    echo "Example: ./scripts/run-pitest.sh cartisan-core"
    exit 1
fi

MODULE=$1
TASK="${MODULE}:pitest"

echo "Running: ./gradlew ${TASK}"
./gradlew "${TASK}"

# 检查报告是否存在
REPORT_DIR="${MODULE}/build/reports/pitest"
if [ ! -d "$REPORT_DIR" ]; then
    echo "❌ PIT report not found at ${REPORT_DIR}"
    exit 1
fi

echo "✅ PIT report generated: ${REPORT_DIR}/index.html"
echo ""
echo "Please check:"
echo "  - Mutation Threshold: should be ≥ 70%"
echo "  - Surviving mutations: review and add tests if needed"
