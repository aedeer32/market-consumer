pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        APP_NAME = 'market-consumer'
        DEPLOY_HOST = 'your-deploy-host'
        DEPLOY_PORT = '22'
        DEPLOY_USER = 'deploy'
        DEPLOY_BASE_DIR = '/opt/market-consumer'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git rev-parse --short HEAD > .git-commit'
            }
        }

        stage('Build & Verify') {
            steps {
                // `verify` runs tests and Spotless check configured in pom.xml.
                sh 'mvn -B clean verify'
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'target/*-jar-with-dependencies.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sshagent(credentials: ['market-consumer-deploy-key']) {
                    sh '''
                        set -eu

                        COMMIT_SHA=$(cat .git-commit)
                        BUILD_TAG="${BUILD_NUMBER}-${COMMIT_SHA}"
                        JAR_PATH=$(ls target/*-jar-with-dependencies.jar | head -n 1)
                        JAR_FILE=$(basename "${JAR_PATH}")
                        REMOTE_RELEASE_DIR="${DEPLOY_BASE_DIR}/releases/${BUILD_TAG}"
                        REMOTE_JAR_PATH="${REMOTE_RELEASE_DIR}/${JAR_FILE}"

                        ssh -p "${DEPLOY_PORT}" -o StrictHostKeyChecking=accept-new "${DEPLOY_USER}@${DEPLOY_HOST}" \
                          "mkdir -p '${REMOTE_RELEASE_DIR}' '${DEPLOY_BASE_DIR}/bin' '${DEPLOY_BASE_DIR}/shared'"

                        scp -P "${DEPLOY_PORT}" -o StrictHostKeyChecking=accept-new "${JAR_PATH}" \
                          "${DEPLOY_USER}@${DEPLOY_HOST}:${REMOTE_JAR_PATH}"

                        scp -P "${DEPLOY_PORT}" -o StrictHostKeyChecking=accept-new scripts/deploy.sh \
                          "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_BASE_DIR}/bin/deploy.sh"

                        ssh -p "${DEPLOY_PORT}" -o StrictHostKeyChecking=accept-new "${DEPLOY_USER}@${DEPLOY_HOST}" \
                          "chmod +x '${DEPLOY_BASE_DIR}/bin/deploy.sh' && \
                           '${DEPLOY_BASE_DIR}/bin/deploy.sh' '${DEPLOY_BASE_DIR}' '${REMOTE_JAR_PATH}' '${APP_NAME}'"
                    '''
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
