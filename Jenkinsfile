pipeline {
  agent any

  options {
    timestamps()
  }

  stages {
    stage('Backend Tests') {
      steps {
        sh 'mvn -Dmaven.repo.local=.m2 test'
        dir('pricing-service') {
          sh 'mvn test'
        }
        dir('notification-service') {
          sh 'mvn test'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Frontend Install') {
      steps {
        dir('web') {
          sh 'npm install'
        }
        dir('admin-web') {
          sh 'npm install'
        }
        dir('e2e') {
          sh 'npm install'
          sh 'npx playwright install chromium'
        }
      }
    }

    stage('Frontend Tests') {
      steps {
        dir('web') {
          sh 'npm run test:ci'
        }
        dir('admin-web') {
          sh 'npm run test:ci'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'web/test-results/*.xml,admin-web/test-results/*.xml'
        }
      }
    }

    stage('Frontend Builds') {
      steps {
        dir('web') {
          sh 'npm run build'
        }
        dir('admin-web') {
          sh 'npm run build'
        }
      }
    }

    stage('E2E Integration Tests') {
      steps {
        sh 'docker compose --env-file .env.integration --profile integration up -d --build'
        sh 'for i in $(seq 1 30); do curl -sk https://localhost:8443/actuator/health && exit 0; sleep 5; done; exit 1'
        dir('e2e') {
          sh 'npm run test'
        }
      }
      post {
        always {
          sh 'docker compose --env-file .env.integration --profile integration down || true'
          junit allowEmptyResults: true, testResults: 'e2e/test-results/*.xml'
          archiveArtifacts allowEmptyArchive: true, artifacts: 'e2e/test-results/**,e2e/playwright-report/**'
        }
      }
    }
  }
}
