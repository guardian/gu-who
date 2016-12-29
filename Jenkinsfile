timestamps {
    node {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {

            stage ('Checkout') {
                checkout scm
                commit_sha = sh (returnStdout: true, script: "git rev-parse HEAD").trim()
                gu_who = docker.image("build-tools.learnvest.net:5000/gu_who:${commit_sha}")
                gu_who_latest = docker.image("build-tools.learnvest.net:5000/gu_who:latest")
            }

            stage ('Test and Build') {
                try {
                    milestone 1
                    // Don't build if this tag already exist on the registry
                    def response1 = httpRequest url: "https://build-tools.learnvest.net:5000/v2/gu_who/manifests/${commit_sha}", validResponseCodes: '100:404'
                    if (response1.status == 404 ){
                        sh 'docker-compose -p gu_who build'
                    }
                    slackSend channel: '#devtools', color: 'good', message: "Build <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_ID}> finished successfully"
                    currentBuild.result = 'SUCCESS'
                } catch (hudson.AbortException e) {
                    echo 'Build aborted'
                    currentBuild.result = 'FAILURE'
                    slackSend channel: '#devtools', color: 'danger', message: "Build <${env.BUILD_URL}/console|${env.JOB_NAME} #${env.BUILD_ID}> aborted"
                } catch (err) {
                    echo 'Testing Failed!'
                    currentBuild.result = 'FAILURE'
                    slackSend channel: '#devtools', color: 'danger', message: "Build <${env.BUILD_URL}/console|${env.JOB_NAME} #${env.BUILD_ID}> failed with errors"
                }
            }

            if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'DEVINFRA-218-gu-who' && currentBuild.result != 'FAILURE') {
                stage ('Push Dev Images') {
                    lock(resource: 'gu-who-push-dev-image', inversePrecedence: true) {
                        milestone 2
                        try {
                            sh "docker tag guwho_gu_who:latest ${gu_who.imageName()}"

                            gu_who.push()
                            gu_who.push('dev')
                        }
                        catch (err) {
                            slackSend channel: '#devtools', color: 'danger', message: "Build ${env.JOB_NAME} #${env.BUILD_ID}: Pushing dev images to docker registry failed"
                            error 'Pushing prod images to docker registry failed'
                        }
                    }
                }
            }
        }
    }

    if (env.BRANCH_NAME == 'master' && currentBuild.result == 'SUCCESS') {
        milestone 3
        jenkins_endpoint = "http://build-tools.learnvest.net/dm/v/api/trigger_jenkins_input/${env.JOB_NAME}/${env.BUILD_ID}/Deploy-approval"
        jenkins_endpoint_approve = "${jenkins_endpoint}/proceedEmpty"
        jenkins_endpoint_abort = "${jenkins_endpoint}/abort"

        slackSend channel: '#devtools', color: 'good', message: "Deploy ${env.BRANCH_NAME} #${env.BUILD_ID} to Production?\n<${jenkins_endpoint_approve}|Yes> - <${jenkins_endpoint_abort}|No>" , teamDomain: 'learnvest', token: '2RIICdBhS6rzSK2BXmbU4GQC'
        input id: 'deploy-approval', message: 'Deploy to Production?', ok: 'Deploy'

        node {
            milestone 4
            stage ('Push Prod Images') {
                checkout scm
                lock(resource: 'gu-who-push-prod-image', inversePrecedence: true) {
                    try {
                        gu_who.pull()
                        gu_who.push('latest')
                    }
                    catch (err) {
                        slackSend channel: '#devtools', color: 'danger', message: "Build ${env.JOB_NAME} #${env.BUILD_ID}: Pushing prod images to docker registry failed"
                        error 'Pushing prod images to docker registry failed'
                    }
                }
            }
            stage ('Deploy Production') {

                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '71ebb556-7ad0-4247-b06f-8ecbede720bb', passwordVariable: 'PW', usernameVariable: 'USER']]) {
                    gu_who_latest.inside {
                        def return_status_deploy = sh (script: "dm trigger_deploy --environment_id 40 --project_name gu-who --sha ${commit_sha} --username ${env.USER} --password ${env.PW}",
                                returnStatus: true)

                        if (return_status_deploy != 0) {
                            slackSend channel: '#devtools', color: 'danger', message: "Build ${env.JOB_NAME} #${env.BUILD_ID}: Deployment to production using trigger_update.py failed"
                            error 'Deployment to production using trigger_update.py failed'
                        }
                    }
                }
            }
        }
    }
}
