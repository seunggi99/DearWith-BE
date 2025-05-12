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
        // dearwith-backend 폴더로 들어가서 gradlew 실행
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
	      sh '/usr/local/bin/docker --version'  // 확인용
	      script {
	        docker.withRegistry('', env.DOCKER_CREDS) {
	          def img = docker.build("${IMAGE_NAME}:${TAG}", "-f docker/Dockerfile .")
	          img.push()
	          img.push('latest')
        	}
      	}
    	}
  	}
}

    stage('Deploy') {
      steps {
        sshagent (credentials: ['deploy-ssh']) {
          sh '''
            ssh -o StrictHostKeyChecking=no $DEPLOY_USER@$DEPLOY_HOST << 'EOF'
              cd ~/DearWith-BE/dearwith-backend
              echo "TAG=${TAG}" > .env
              echo "MONGO_USER=${MONGO_USER}" >> .env
              echo "MONGO_PASS=${MONGO_PASS}" >> .env
              docker-compose pull
              docker-compose up -d --remove-orphans
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