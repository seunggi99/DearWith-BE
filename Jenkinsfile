pipeline {
  agent any

    tools {
    dockerTool 'docker'    
  }

  environment {
    GIT_CREDS    = 'github-ssh'
    DOCKER_CREDS = 'docker-hub'
    IMAGE_NAME   = 'seunggi99/dearwith-be'
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
          sh '/usr/local/bin/docker build -t ninny9988/dearwith-be:latest .'
          sh '/usr/local/bin/docker login -u ninny9988 -p ${DOCKER_PASSWORD}'
          sh '/usr/local/bin/docker push ninny9988/dearwith-be:latest'
        }
      }
    }

    stage('Deploy') {
      steps {
        sshagent(['ec2-ssh-key']) {
          sh '''
            ssh -o StrictHostKeyChecking=no ec2-user@${EC2_HOST} << 'EOF'
              cd ~/DearWith-BE/dearwith-backend
              docker pull ninny9988/dearwith-be:latest
              docker-compose up -d
            EOF
          '''
        }
      }
    }
  }

  post {
    success { echo '🎉 Pipeline succeeded!' }
    failure { echo '❌ Pipeline failed.' }
  }
}