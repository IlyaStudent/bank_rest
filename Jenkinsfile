pipeline {
    agent any

    environment {
        APP_IMAGE = 'bank-rest:latest'
        NAMESPACE = 'bank-rest'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Branch: ${env.BRANCH_NAME ?: 'unknown'}"
                echo "Commit: ${env.GIT_COMMIT ?: 'unknown'}"
            }
        }

        stage('Build & Test') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean verify -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
            }
            post {
                always {
                    junit(
                            testResults: '**/target/surefire-reports/*.xml',
                            allowEmptyResults: true
                    )
                }
            }
        }

        stage('Docker build') {
            steps {
                script {
                    sh '''
                        eval $(minikube docker-env)
                        docker build -t ${APP_IMAGE} .
                        echo "Docker image built: ${APP_IMAGE}"
                        docker images | grep bank-rest
                    '''
                }
            }
        }

        stage('Deploy to k8s') {
            steps {
                sh '''
                echo "Applying Kubernetes manifests..."

                    kubectl apply -f k8s/namespace.yml
                    kubectl apply -f k8s/secrets.yml
                    kubectl apply -f k8s/configmap.yml
                    kubectl apply -f k8s/postgres.yml
                    kubectl apply -f k8s/redis.yml
                    kubectl apply -f k8s/zookeeper.yml
                    kubectl apply -f k8s/kafka.yml
                    kubectl apply -f k8s/app.yml

                    echo "Restarting app deployment..."
                    kubectl rollout restart deployment/bank-rest-app -n ${NAMESPACE}

                    echo "Waiting for rollout to complete..."
                    kubectl rollout status deployment/bank-rest-app -n ${NAMESPACE} --timeout=300s
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "=== Pod Status ==="
                    kubectl get pods -n ${NAMESPACE} -o wide

                    echo ""
                    echo "=== Services ==="
                    kubectl get svc -n ${NAMESPACE}

                    echo ""
                    echo "=== Health Check ==="
                    MINIKUBE_IP=$(minikube ip)
                    MAX_RETRIES=10
                    RETRY_COUNT=0

                    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
                        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://${MINIKUBE_IP}:30080/actuator/health || true)
                        if [ "$HTTP_CODE" = "200" ]; then
                            echo "Health check PASSED (HTTP 200)"
                            curl -s http://${MINIKUBE_IP}:30080/actuator/health | head -c 500
                            echo ""
                            exit 0
                        fi
                        RETRY_COUNT=$((RETRY_COUNT + 1))
                        echo "Attempt ${RETRY_COUNT}/${MAX_RETRIES}: HTTP ${HTTP_CODE}, retrying in 10s..."
                        sleep 10
                    done

                    echo "ERROR: Health check FAILED after ${MAX_RETRIES} attempts"
                    kubectl logs -l app=bank-rest-app -n ${NAMESPACE} --tail=50
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            node('') {
                echo "Pipeline completed successfully"
                sh 'MINIKUBE_IP=$(minikube ip) && echo "App URL: http://${MINIKUBE_IP}:30080"'
            }
        }
        failure {
            node('') {
                echo "Pipeline FAILED"
                sh '''
                    echo "=== Failed Pod Logs ==="
                    kubectl logs -l app=bank-rest-app -n ${NAMESPACE} --tail=100 || true
                    echo ""
                    echo "=== Pod Events ==="
                    kubectl describe pods -l app=bank-rest-app -n ${NAMESPACE} || true
                '''
            }
        }
        always {
            node('') {
                cleanWs()
            }
        }
    }
}