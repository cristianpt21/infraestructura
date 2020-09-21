pipeline {
    environment {
        BASE_REPO_URL = 'https://github.com/cristianpt21'
        APP_REPO_URL = "${env.BASE_REPO_URL}/${repo}.git"
        INFRA_REPO_URL = "${env.BASE_REPO_URL}/infraestructura.git"
        DOCKER_IMAGE = "crisapt1/${repo}"
        DEPLOY_FOLDER = "deploy/kubernete/${repo}"
    }
    agent any
    stages {
        stage("Checkout app-code") {
            steps {
            //se esta crando una carpeta....
                dir('app') {
                    git url:"${env.APP_REPO_URL}" , branch: "${version}"
                } 
            }
        }
        stage("Checkout deploy-code") {
            steps {
                dir('deploy') {
                    git url:"${env.INFRA_REPO_URL}" , branch: "master"
                } 
            }
        }
        stage("Build image") {
            steps {
                dir('app') {
                    script {
                        dockerImage = docker.build("${env.DOCKER_IMAGE}:${tag}")
                    }
                }
            }
        }
        stage("Push image") {
            steps {
                script {
                    docker.withRegistry('', 'dockercristian') {
                        dockerImage.push()
                    }
                }
            }
        }
        stage('Deploy') {
            steps{
                sh "sed -i 's:DOCKER_IMAGE:${env.DOCKER_IMAGE}:g' ${DEPLOY_FOLDER}/deployment.yaml"
                sh "sed -i 's:TAG:${tag}:g' ${DEPLOY_FOLDER}/deployment.yaml"
                
                step([$class: 'KubernetesEngineBuilder',
                projectId: "airy-actor-288822",
                clusterName: "cluster-test",
                zone: "us-central1-a",
                manifestPattern: "${DEPLOY_FOLDER}/",
                credentialsId: "infraestructura",
                verifyDeployments: true])
            }
        }
    }
}
