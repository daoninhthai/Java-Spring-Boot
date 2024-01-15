pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    environment {
        DOCKER_REGISTRY = credentials('docker-registry-url')
        DOCKER_CREDENTIALS = credentials('docker-registry-credentials')
        IMAGE_NAME = 'spring-cloud-gateway'
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'unknown'}"
        MAVEN_OPTS = '-Xmx1024m'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_BRANCH_NAME = env.GIT_BRANCH?.replaceAll('origin/', '') ?: 'unknown'
                    echo "Building branch: ${env.GIT_BRANCH_NAME}"
                    echo "Commit: ${env.GIT_COMMIT}"
                }
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling application...'
                sh 'mvn clean compile -B -q'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn test -B'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    jacoco(
                        execPattern: 'target/jacoco.exec',
                        classPattern: 'target/classes',
                        sourcePattern: 'src/main/java',
                        exclusionPattern: '**/config/**,**/dto/**'
                    )
                }
            }
        }

        stage('Integration Tests') {
            steps {
                echo 'Running integration tests...'
                sh 'mvn verify -DskipUnitTests -B'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging application...'
                sh 'mvn package -DskipTests -B'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('SonarQube Analysis') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar -B'
                }
            }
        }

        stage('Docker Build') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch pattern: 'release/*', comparator: 'GLOB'
                }
            }
            steps {
                echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                script {
                    docker.build("${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}", '.')
                    docker.build("${DOCKER_REGISTRY}/${IMAGE_NAME}:latest", '.')
                }
            }
        }

        stage('Docker Push') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch pattern: 'release/*', comparator: 'GLOB'
                }
            }
            steps {
                echo "Pushing Docker image to registry..."
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        docker.image("${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}").push()

                        if (env.GIT_BRANCH_NAME == 'main') {
                            docker.image("${DOCKER_REGISTRY}/${IMAGE_NAME}:latest").push()
                        }
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                echo 'Deploying to staging environment...'
                sh """
                    kubectl set image deployment/${IMAGE_NAME} \
                        ${IMAGE_NAME}=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \
                        --namespace=staging \
                        --record
                """
                sh "kubectl rollout status deployment/${IMAGE_NAME} --namespace=staging --timeout=120s"
            }
        }
    }

    post {
        success {
            echo "Build #${env.BUILD_NUMBER} succeeded for branch ${env.GIT_BRANCH_NAME}"
            slackSend(
                color: 'good',
                message: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} (${env.GIT_BRANCH_NAME})\n${env.BUILD_URL}"
            )
        }
        failure {
            echo "Build #${env.BUILD_NUMBER} failed for branch ${env.GIT_BRANCH_NAME}"
            slackSend(
                color: 'danger',
                message: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} (${env.GIT_BRANCH_NAME})\n${env.BUILD_URL}"
            )
        }
        always {
            cleanWs()
        }
    }
}
