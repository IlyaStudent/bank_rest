#!/bin/bash
set -e

echo "=========================================="
echo "  Bank REST — Codespaces Setup"
echo "=========================================="

echo ""
echo "[1/4] Starting Minikube..."
minikube start \
    --driver=docker \
    --cpus=2 \
    --memory=4096 \
    --disk-size=20g \
    --kubernetes-version=stable

echo "Minikube status:"
minikube status

echo ""
echo "[2/4] Configuring Docker environment..."
eval $(minikube docker-env)
echo "Docker is now pointing to Minikube's Docker daemon."

echo ""
echo "[3/4] Starting Jenkins..."

# Создаём volume для сохранения данных Jenkins
docker volume create jenkins-data 2>/dev/null || true

# Останавливаем старый контейнер, если есть
docker rm -f jenkins 2>/dev/null || true

# Запускаем Jenkins
docker run -d \
    --name jenkins \
    --restart=unless-stopped \
    -p 8888:8080 \
    -p 50000:50000 \
    -v jenkins-data:/var/jenkins_home \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v $(which kubectl):/usr/local/bin/kubectl \
    -v $HOME/.kube:/var/jenkins_home/.kube:ro \
    -e JAVA_OPTS="-Xmx512m" \
    jenkins/jenkins:lts-jdk21

echo "Jenkins is starting on port 8888..."
echo "Waiting for Jenkins to be ready..."

# Ждём, пока Jenkins стартует
MAX_WAIT=120
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/login 2>/dev/null | grep -q "200"; then
        break
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo "  Waiting... (${ELAPSED}s / ${MAX_WAIT}s)"
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo "WARNING: Jenkins may not be fully ready yet. Check logs: docker logs jenkins"
else
    echo "Jenkins is ready!"
fi

echo ""
echo "[4/4] Jenkins setup info:"
echo ""
echo "  Jenkins URL: http://localhost:8888"
echo ""
echo "  Initial Admin Password:"
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || \
    echo "  (password will be available after Jenkins finishes starting)"
echo ""

echo "=========================================="
echo "  Setup complete!"
echo "=========================================="
echo ""
echo "  Minikube:  Running (2 CPU, 4GB RAM)"
echo "  Jenkins:   http://localhost:8888"
echo "  App:       http://$(minikube ip):30080 (after deployment)"
echo ""
echo "  Next steps:"
echo "    1. Open Jenkins UI and complete initial setup"
echo "    2. Install suggested plugins"
echo "    3. Create Pipeline job pointing to this repo's Jenkinsfile"
echo "    4. Or deploy manually: bash k8s/deploy.sh"
echo ""