#!/bin/bash
# DeskPet 镜像构建脚本
# 用法: ./build.sh [TAG] [REGISTRY]
# 示例: ./build.sh v0.1.0 registry.cn-hangzhou.aliyuncs.com/yournamespace/

set -e

TAG=${1:-latest}
REGISTRY=${2:-}
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=========================================="
echo " DeskPet 镜像构建"
echo " TAG: $TAG"
echo " REGISTRY: ${REGISTRY:-（本地）}"
echo "=========================================="

# 1. Maven 构建
echo ""
echo "[1/6] Maven 打包..."
cd "$PROJECT_ROOT"
mvn clean package -DskipTests -q

# 2. 构建 pet-core
echo "[2/6] 构建 pet-core 镜像..."
docker build -t "${REGISTRY}deskpet/pet-core:${TAG}" "$PROJECT_ROOT/pet-core"

# 3. 构建 mqtt-gateway
echo "[3/6] 构建 mqtt-gateway 镜像..."
docker build -t "${REGISTRY}deskpet/mqtt-gateway:${TAG}" "$PROJECT_ROOT/mqtt-gateway"

# 4. 构建 pet-ai
echo "[4/6] 构建 pet-ai 镜像..."
docker build -t "${REGISTRY}deskpet/pet-ai:${TAG}" "$PROJECT_ROOT/pet-ai"

# 5. 构建 user-frontend
echo "[5/6] 构建 user-frontend 镜像..."
docker build -t "${REGISTRY}deskpet/user-frontend:${TAG}" "$PROJECT_ROOT/deskpetConsole/user-frontend"

# 6. 构建 manager
echo "[6/6] 构建 manager 镜像..."
docker build -t "${REGISTRY}deskpet/manager:${TAG}" "$PROJECT_ROOT/deskpetConsole/manager"

echo ""
echo "=========================================="
echo " 构建完成！镜像列表："
echo "=========================================="
docker images | grep deskpet | grep "$TAG"

# 推送镜像（如果指定了 REGISTRY）
if [ -n "$REGISTRY" ]; then
    echo ""
    read -p "是否推送镜像到 ${REGISTRY}？(y/N) " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        echo "推送镜像..."
        docker push "${REGISTRY}deskpet/pet-core:${TAG}"
        docker push "${REGISTRY}deskpet/mqtt-gateway:${TAG}"
        docker push "${REGISTRY}deskpet/pet-ai:${TAG}"
        docker push "${REGISTRY}deskpet/user-frontend:${TAG}"
        docker push "${REGISTRY}deskpet/manager:${TAG}"
        echo "推送完成！"
    fi
fi
