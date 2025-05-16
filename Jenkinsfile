pipeline {
  agent any

  tools {
    dockerTool 'docker'
  }

  environment {
    GIT_CREDS    = 'github-ssh'
    DOCKER_CREDS = 'docker-hub'
    IMAGE_NAME   = 'ninny9988/dearwith-be'
    TAG          = "${env.GIT_COMMIT}"

    DEPLOY_HOST  = 'ec2-3-37-184-199.ap-northeast-2.compute.amazonaws.com'
    DEPLOY_USER  = 'ec2-user'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([
          $class: 'GitSCM',
          branches: [[ name: '*/main' ]],
          userRemoteConfigs: [[
            url: 'git@github.com:seunggi99/DearWith-BE.git',
            credentialsId: env.GIT_CREDS
          ]]
        ])
      }
    }

    stage('Build & Test') {
      steps {
        dir('dearwith-backend') {
          sh './gradlew clean bootJar -x test'
          sh './gradlew test'
        }
      }
    }

    stage('Debug PATH') {
      steps {
        sh 'echo ">>>> PATH is: $PATH"'
      }
    }

    stage('Docker Build & Push') {
      steps {
        dir('dearwith-backend') {
          // 플랫폼 명시하여 빌드
          sh 'docker build --platform=linux/amd64 -t ${IMAGE_NAME}:latest .'

          withCredentials([usernamePassword(
            credentialsId: env.DOCKER_CREDS,
            usernameVariable: 'DOCKER_USERNAME',
            passwordVariable: 'DOCKER_PASSWORD'
          )]) {
            sh 'echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin'
          }

          sh 'docker push ${IMAGE_NAME}:latest'
        }
      }
    }

    stage('Deploy') {
      steps {
        sshagent(['ec2-ssh-key']) {
          sh """
ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} << 'EOF'
cd ~/DearWith-BE/dearwith-backend
docker pull ${IMAGE_NAME}:latest
docker-compose down
docker-compose up -d
EOF
          """
        }
      }
    }
  }

  post {
    success { echo '🎉 Pipeline succeeded!' }
    failure { echo '❌ Pipeline failed.' }
  }
}