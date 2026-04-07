#!/bin/bash
# PIT 变异测试脚本
# 使用 Maven PIT 插件运行

set -e

cd "$(dirname "$0")"

echo "=== PIT Mutation Testing ==="
echo "Running PIT with Maven..."
echo ""
