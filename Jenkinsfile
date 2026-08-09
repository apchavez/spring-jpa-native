// Jenkins Declarative Pipeline equivalent of .github/workflows/ci.yml.
// Kept alongside GitHub Actions (the CI actually enforced on this repo) to demonstrate
// Jenkinsfile/Groovy DSL fluency for orgs that run Jenkins on-prem instead of a SaaS CI.
pipeline {
    agent any

    tools {
        jdk 'temurin-21'
        nodejs 'node20'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('API — build, test (Testcontainers), coverage') {
            steps {
                dir('api') {
                    sh '''
                        chmod +x gradlew
                        ./gradlew check --no-daemon
                    '''
                }
            }
        }

        stage('SonarCloud analysis') {
            when {
                branch 'main'
            }
            steps {
                withSonarQubeEnv('SonarCloud') {
                    dir('api') {
                        sh './gradlew sonar --no-daemon'
                    }
                }
            }
        }

        stage('Mobile — lint & typecheck (Expo)') {
            when {
                expression { fileExists('mobile/package.json') }
            }
            steps {
                dir('mobile') {
                    sh '''
                        npm ci
                        npx jest --coverage --ci
                        npx expo-doctor
                        npx tsc --noEmit
                    '''
                }
            }
        }

        stage('Docker build & push') {
            when {
                branch 'main'
            }
            steps {
                sh 'docker build -t ghcr.io/apchavez/spring-jpa-native-api:latest ./api'
            }
        }
    }

    post {
        always {
            junit testResults: 'api/build/test-results/**/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'api/build/reports/jacoco/test/html/**', allowEmptyArchive: true
        }
    }
}
