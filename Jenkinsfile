pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        APP_NAME = 'market-consumer'
        JAR_NAME = 'market-consumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
        JAR_PATH = "target/${JAR_NAME}"

        DEPLOY_HOST = 'your-wsl-host'
        DEPLOY_PORT = '22'
        DEPLOY_USER = 'pivot19'
        DEPLOY_BASE_DIR = '/opt/market-consumer'
    }

    triggers {
        // GitHub webhook を使うなら通常ここは不要
        // pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git rev-parse --short HEAD > .git-commit'
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn -B clean test package'
            }
        }

        stage('Docker Build') {
          steps {
            sh 'docker build -t market-consumer:latest .'
          }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy to WSL') {
            when {
                branch 'main'
            }
            steps {
                sshagent(credentials: ['market-consumer-deploy-key']) {
                    sh '''
                        set -eux

                        COMMIT_SHA=$(cat .git-commit)
                        BUILD_TAG_SAFE="${BUILD_NUMBER}-${COMMIT_SHA}"
                        REMOTE_RELEASE_JAR="${DEPLOY_BASE_DIR}/releases/${APP_NAME}-${BUILD_TAG_SAFE}.jar"

                        ssh -p ${DEPLOY_PORT} -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                          "mkdir -p ${DEPLOY_BASE_DIR}/releases ${DEPLOY_BASE_DIR}/current ${DEPLOY_BASE_DIR}/shared/logs ${DEPLOY_BASE_DIR}/bin"

                        scp -P ${DEPLOY_PORT} -o StrictHostKeyChecking=no ${JAR_PATH} \
                          ${DEPLOY_USER}@${DEPLOY_HOST}:${REMOTE_RELEASE_JAR}

                        scp -P ${DEPLOY_PORT} -o StrictHostKeyChecking=no scripts/deploy.sh \
                          ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_BASE_DIR}/bin/deploy.sh

                        ssh -p ${DEPLOY_PORT} -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                          "chmod +x ${DEPLOY_BASE_DIR}/bin/deploy.sh && \
                           ${DEPLOY_BASE_DIR}/bin/deploy.sh ${DEPLOY_BASE_DIR} ${APP_NAME}-${BUILD_TAG_SAFE}.jar"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed.'
        }
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}