#!/usr/bin/env groovy

import java.text.SimpleDateFormat

pipeline {
    agent any

    environment {
        APPLICATION_NAME = 'syfooversiktsrv'
    }

    stages {
        stage('initialize') {
            steps {
                script {
                    sh './gradlew clean'
                    def date = new Date()
                    def dateFormat = new SimpleDateFormat("dd.MM.HHmm")
                    env.COMMIT_HASH_SHORT = "${env.GIT_COMMIT}"[0..6]
                    env.APPLICATION_VERSION = dateFormat.format(date) + "-${env.COMMIT_HASH_SHORT}"
                }
            }
        }
        stage('build') {
            steps {
                sh './gradlew build -x test'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh './gradlew test'
            }
        }
        stage('create uber jar') {
            steps {
                sh './gradlew shadowJar'
            }
        }
        stage('push docker image') {
            steps {
                script {
                    docker.withRegistry('https://repo.adeo.no:5443', 'nexus-credentials') {
                        def image = docker.build("syfo/${APPLICATION_NAME}:${APPLICATION_VERSION}", "--pull --build-arg GIT_COMMIT_ID=${env.COMMIT_HASH_SHORT} --build-arg http_proxy=http://webproxy-internett.nav.no:8088 --build-arg https_proxy=http://webproxy-internett.nav.no:8088 .")
                        image.push()
                    }
                }
            }
        }
        stage('update version in naiserator.yaml') {
            steps {
                script {
                    withEnv(['HTTPS_PROXY=http://webproxy-internett.nav.no:8088']) {
                        withCredentials([string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
                            def naiseratorFile = sh(script: "curl -s https://${token}@raw.githubusercontent.com/navikt/syfonais/master/preprod-fss/syfooversiktsrv/naiserator.yaml", returnStdout: true).trim()
                            writeFile file: "naiserator.yaml", text: naiseratorFile
                            def payload = readFile('naiserator.yaml').replaceAll("@@version@@", env.APPLICATION_VERSION)
                            writeFile file: "naiserator.yaml", text: payload
                        }
                    }
                }
            }
        }

        stage('deploy to preprod') {
            steps {
                script {
                    withCredentials([file(credentialsId: env.KUBECONFIG ?: 'kubeconfig', variable: 'KUBECONFIG')]) {
                        sh 'kubectl apply --context preprod-fss --namespace default -f naiserator.yaml'
                        sh 'kubectl --context preprod-fss --namespace default rollout status -w deployment/${APPLICATION_NAME}'
                    }
                }
            }
        }
    }
}
