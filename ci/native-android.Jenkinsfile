import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

def canonicalJob = 'Team_Mobile_Messaging/infobip-mobile-messaging-android'
properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '30')),
    parameters([
        string(name: 'BRANCH_NAME_TO_BUILD', defaultValue: 'master', trim: true),
        booleanParam(name: 'WITH_SQ_REPORT', defaultValue: true, description: 'Run coverage and SonarQube analysis.'),
        choice(name: 'MAC_AGENT', choices: ['auto', 'ci-zg-mac-mini-02', 'ci-zg-mac-mini-03'], description: 'Auto selects an online Mac with a free device resource; prefers mini-02.'),
        booleanParam(name: 'DONT_PUSH_ON_GITHUB', defaultValue: false, description: 'Skip the source mirror. No SDK release is performed by this job.'),
        booleanParam(name: 'SCHEDULER_CHECK_ONLY', defaultValue: false, description: 'Validate scheduling only; no SDK, emulator or publishing actions.')
    ])
])

def hosts = [
    'ci-zg-mac-mini-02': [resource: 'mmt-mac-mini-02-devices', worker: 'Team_Mobile_Messaging/native-android-workers/mac-mini-02'],
    'ci-zg-mac-mini-03': [resource: 'mmt-mac-mini-03-devices', worker: 'Team_Mobile_Messaging/native-android-workers/mac-mini-03']
]
def requestedHost = params.MAC_AGENT ?: 'auto'
def candidates = requestedHost == 'auto' ? hosts.keySet().toList() : [requestedHost]
if (!candidates.every { hosts.containsKey(it) }) { error('Unknown native Android agent') }
def branch = (params.BRANCH_NAME_TO_BUILD ?: 'master').trim()
if (!branch || branch.contains('\n') || branch.contains('\r')) { error('Invalid branch') }
def completed = false
def metadata = null

timestamps {
    try {
        timeout(time: 4, unit: 'HOURS') {
            // Also acquired by the release job: never publish/test two native
            // Android builds concurrently, even when they use different Macs.
            lock(resource: 'mmt-native-android-build') {
                stage('Select available Mac') {
                    waitUntil(initialRecurrencePeriod: 10000, quiet: true) {
                        def online = nodesByLabel(label: 'ci-zg-mac-mini-02 || ci-zg-mac-mini-03', offline: false)
                        for (host in candidates) {
                            if (completed) { break }
                            if (!online.contains(host)) { continue }
                            def target = hosts[host]
                            lock(resource: target.resource, skipIfLocked: true) {
                                echo('Selected ' + host + ' under ' + target.resource)
                                if (params.SCHEDULER_CHECK_ONLY) {
                                    timeout(time: 1, unit: 'MINUTES') {
                                        node(host) {
                                            echo('SCHEDULER_CHECK node=' + env.NODE_NAME + ' resource=' + target.resource)
                                        }
                                    }
                                    currentBuild.displayName = '#' + env.BUILD_NUMBER + ': scheduler check (' + host + ')'
                                    currentBuild.description = 'Scheduling check only. Selected ' + host + ' with ' + target.resource + '.'
                                    completed = true
                                } else {
                                    def workerRun = null
                                    def workerParameters = [
                                        string(name: 'BRANCH_NAME_TO_BUILD', value: branch),
                                        booleanParam(name: 'WITH_SQ_REPORT', value: params.WITH_SQ_REPORT == null ? true : params.WITH_SQ_REPORT),
                                        booleanParam(name: 'DONT_PUSH_ON_GITHUB', value: params.DONT_PUSH_ON_GITHUB ?: false),
                                        string(name: 'CI_PARENT_JOB', value: env.JOB_NAME),
                                        string(name: 'CI_PARENT_BUILD', value: env.BUILD_TAG),
                                        string(name: 'CI_DEVICE_RESOURCE', value: target.resource)
                                    ]
                                    try {
                                        timeout(time: 1, unit: 'MINUTES') {
                                            workerRun = build(job: '/' + target.worker, parameters: workerParameters,
                                                quietPeriod: 0, waitForStart: true, propagate: false)
                                        }
                                    } catch (FlowInterruptedException interrupted) {
                                        if (interrupted.isActualInterruption() || workerRun != null) { throw interrupted }
                                        echo('Worker could not start promptly on ' + host + '; trying another available Mac.')
                                    }
                                    if (workerRun != null) {
                                        stage('Native Android build on ' + host) {
                                            workerRun = waitForBuild(runId: workerRun.externalizableId, propagate: false, propagateAbort: true)
                                        }
                                        // Never retry a build that has started, including test or publishing failures.
                                        completed = true
                                        currentBuild.displayName = '#' + env.BUILD_NUMBER + ': ' + branch + ' (' + host + ')'
                                        currentBuild.description = 'Built on ' + host + '. <a href="' + workerRun.absoluteUrl + '">Worker #' + workerRun.number + ' and full console</a>.'
                                        currentBuild.result = workerRun.result
                                        stage('Publish Android results') {
                                            node(host) {
                                                dir('.ci-dispatch-reports/' + env.BUILD_NUMBER) {
                                                    try {
                                                        deleteDir()
                                                        copyArtifacts(projectName: '/' + target.worker, selector: specific(workerRun.number.toString()),
                                                            filter: 'ci-artifacts/**,**/build/outputs/androidTest-results/connected/**/TEST-*.xml,**/build/test-results/**/TEST-*.xml,build/emulator-startup.log,build/logcat.txt', optional: true)
                                                        if (fileExists('ci-artifacts/dispatch.json')) {
                                                            metadata = readJSON(file: 'ci-artifacts/dispatch.json', returnPojo: true)
                                                            if (metadata.node != host || metadata.resource != target.resource || metadata.parent != env.BUILD_TAG) {
                                                                error('Worker result does not match the reserved Mac/build')
                                                            }
                                                            if (!(metadata.commit ==~ /[0-9a-f]{40}/)) {
                                                                error('Worker did not identify a valid tested SDK commit')
                                                            }
                                                            currentBuild.description += ' Tested SDK commit: ' + metadata.commit + '.'
                                                        } else if (workerRun.result == 'SUCCESS') {
                                                            error('Successful worker did not publish dispatch metadata')
                                                        }
                                                        archiveArtifacts(artifacts: '**/*', allowEmptyArchive: workerRun.result != 'SUCCESS')
                                                        junit(testResults: '**/build/outputs/androidTest-results/connected/**/TEST-*.xml,**/build/test-results/**/TEST-*.xml',
                                                            allowEmptyResults: workerRun.result != 'SUCCESS')
                                                    } finally {
                                                        deleteDir()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return completed
                    }
                }
            }
        }
    } catch (FlowInterruptedException interrupted) {
        currentBuild.result = 'ABORTED'
        throw interrupted
    } catch (Exception failure) {
        currentBuild.result = 'FAILURE'
        throw failure
    } finally {
        if (env.JOB_NAME == canonicalJob && metadata?.commit) {
            notifyBitbucket(commitSha1: metadata.commit)
        }
        if (env.JOB_NAME == canonicalJob && !params.SCHEDULER_CHECK_ONLY) {
            def previous = currentBuild.previousBuild?.result
            if (currentBuild.currentResult == 'FAILURE' && previous != 'FAILURE') {
                slackSend(channel: 'mmt_mobile_jenkins_failures', color: 'danger', failOnError: false,
                    message: 'Android build failed: <' + env.BUILD_URL + '|' + env.JOB_NAME + ' #' + env.BUILD_NUMBER + '> (' + branch + ')')
            } else if (currentBuild.currentResult == 'SUCCESS' && previous in ['FAILURE', 'UNSTABLE']) {
                slackSend(channel: 'mmt_mobile_jenkins_failures', color: 'good', failOnError: false,
                    message: 'Android build is back to normal: <' + env.BUILD_URL + '|' + env.JOB_NAME + ' #' + env.BUILD_NUMBER + '>')
            }
        }
    }
}
