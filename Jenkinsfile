pipeline {
  agent any

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build') {
      steps {
        sh 'mvn clean test package'
      }
    }

    stage('Docker Build') {
      steps {
        sh 'docker build -t market-consumer:latest .'
      }
    }
  }
}