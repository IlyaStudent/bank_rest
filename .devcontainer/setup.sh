#!/bin/bash
set -e

echo "=========================================="
echo "  Bank REST — Codespaces Setup"
echo "=========================================="

echo ""
echo "[1/5] Starting Minikube..."
minikube start \
    --driver=docker \
    --cpus=2 \
    --memory=4096 \
    --disk-size=20g \
    --kubernetes-version=stable

echo "Minikube status:"
minikube status

MINIKUBE_IP=$(minikube ip)
echo "Minikube IP: ${MINIKUBE_IP}"

echo ""
echo "[2/5] Starting Jenkins..."

# Create volume for Jenkins data
docker volume create jenkins-data 2>/dev/null || true

# Stop old container if exists
docker rm -f jenkins 2>/dev/null || true

# Start Jenkins on minikube network so it can reach Minikube's Docker daemon and K8s API
docker run -d \
    --name jenkins \
    --restart=unless-stopped \
    --network=minikube \
    -p 8888:8080 \
    -p 50000:50000 \
    -v jenkins-data:/var/jenkins_home \
    -v "$(which kubectl)":/usr/local/bin/kubectl \
    -v "$HOME/.kube":/var/jenkins_home/.kube:ro \
    -v "$HOME/.minikube":"$HOME/.minikube":ro \
    -e DOCKER_HOST="tcp://${MINIKUBE_IP}:2376" \
    -e DOCKER_TLS_VERIFY="1" \
    -e DOCKER_CERT_PATH="$HOME/.minikube/certs" \
    -e JAVA_OPTS="-Xmx512m" \
    jenkins/jenkins:lts-jdk21

echo ""
echo "[3/5] Installing Docker CLI in Jenkins..."
sleep 5

# Install Docker CLI (static binary) into the Jenkins container
docker exec -u root jenkins sh -c "\
    curl -fsSL https://download.docker.com/linux/static/stable/\$(uname -m)/docker-25.0.3.tgz \
    | tar xz --strip-components=1 -C /usr/local/bin docker/docker \
    && docker version --format 'Docker CLI {{.Client.Version}} installed'"

echo ""
echo "[4/5] Waiting for Jenkins..."

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
echo "Jenkins setup info:"
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
echo "  App:       http://${MINIKUBE_IP}:30080 (after deployment)"
echo ""
echo "  Next steps:"
echo "    1. Open Jenkins UI and complete initial setup"
echo "    2. Install suggested plugins"
echo "    3. Create Pipeline job pointing to this repo's Jenkinsfile"
echo "    4. Or deploy manually: bash k8s/deploy.sh"
echo ""
