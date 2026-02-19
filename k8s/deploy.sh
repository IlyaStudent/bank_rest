#!/bin/bash
set -e

NAMESPACE="bank-rest"
APP_IMAGE="bank-rest:latest"

echo "=========================================="
echo "  Bank REST — Kubernetes Deployment"
echo "=========================================="

# 1. Проверяем, что Minikube запущен
echo ""
echo "[1/7] Checking Minikube status..."
if ! minikube status | grep -q "Running"; then
    echo "ERROR: Minikube is not running. Start it first:"
    echo "  minikube start --cpus=2 --memory=4096"
    exit 1
fi

# 2. Переключаемся на Docker Minikube
echo "[2/7] Switching to Minikube Docker environment..."
eval $(minikube docker-env)

# 3. Собираем Docker-образ
echo "[3/7] Building Docker image: ${APP_IMAGE}..."
docker build -t ${APP_IMAGE} .

# 4. Применяем namespace
echo "[4/7] Applying namespace..."
kubectl apply -f k8s/namespace.yml

# 5. Применяем secrets и configmap
echo "[5/7] Applying secrets and configmap..."
kubectl apply -f k8s/secrets.yml
kubectl apply -f k8s/configmap.yml

# 6. Применяем инфраструктуру и приложение
echo "[6/7] Applying deployments and services..."
kubectl apply -f k8s/postgres.yml
kubectl apply -f k8s/redis.yml
kubectl apply -f k8s/zookeeper.yml
kubectl apply -f k8s/kafka.yml
kubectl apply -f k8s/app.yml

# 7. Ждём готовности
echo "[7/7] Waiting for pods to be ready..."
echo ""

echo "--- Waiting for PostgreSQL..."
kubectl wait --for=condition=ready pod -l app=postgres -n ${NAMESPACE} --timeout=120s

echo "--- Waiting for Redis..."
kubectl wait --for=condition=ready pod -l app=redis -n ${NAMESPACE} --timeout=120s

echo "--- Waiting for Zookeeper..."
kubectl wait --for=condition=ready pod -l app=zookeeper -n ${NAMESPACE} --timeout=120s

echo "--- Waiting for Kafka..."
kubectl wait --for=condition=ready pod -l app=kafka -n ${NAMESPACE} --timeout=180s

echo "--- Waiting for Bank REST App..."
kubectl wait --for=condition=ready pod -l app=bank-rest-app -n ${NAMESPACE} --timeout=300s

echo ""
echo "=========================================="
echo "  All pods are running!"
echo "=========================================="
echo ""

# Показываем статус
kubectl get pods -n ${NAMESPACE}
echo ""

# Показываем URL доступа
MINIKUBE_IP=$(minikube ip)
echo "Application URL: http://${MINIKUBE_IP}:30080"
echo "Health check:    http://${MINIKUBE_IP}:30080/actuator/health"
echo "Swagger UI:      http://${MINIKUBE_IP}:30080/swagger-ui.html"