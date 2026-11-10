// Run using sandboxed Job DSL with an administrator-configured run identity.
// The template keeps credentials inside Jenkins. Inspect generated, disabled
// workers before enabling them; never regenerate an active worker.
def template = binding.hasVariable('templateJob') ? templateJob : 'Team_Mobile_Messaging/infobip-mobile-messaging-android-release'
def allowedParents = 'Team_Mobile_Messaging/infobip-mobile-messaging-android(-dispatcher)?'
def parentNames = ['Team_Mobile_Messaging/infobip-mobile-messaging-android',
                   'Team_Mobile_Messaging/infobip-mobile-messaging-android-dispatcher']
def artifactPattern = 'ci-artifacts/**,**/build/outputs/androidTest-results/connected/**/TEST-*.xml,**/build/test-results/**/TEST-*.xml,build/emulator-startup.log,build/logcat.txt'

folder('Team_Mobile_Messaging/native-android-workers') {
    description('Internal Android workers and push relay. Start SDK builds through infobip-mobile-messaging-android.')
}

['02', '03'].each { suffix ->
    def host = 'ci-zg-mac-mini-' + suffix
    def resource = 'mmt-mac-mini-' + suffix + '-devices'
    freeStyleJob('Team_Mobile_Messaging/native-android-workers/mac-mini-' + suffix) {
        using(template)
        disabled(true)
        concurrentBuild(false)
        label(host)
        customWorkspace('workspace/Team_Mobile_Messaging/native-android-pool/mac-mini-' + suffix + '/infobip-mobile-messaging-android')
        description('Internal Android worker for ' + host + '. Use the dispatcher, which owns the native-build and device locks through completion.')
        configure { project ->
            project.triggers[0].children().clear()
            def props = project.properties[0]
            props.children().removeAll {
                it.name() in ['org.jenkins.plugins.lockableresources.RequiredResourcesProperty',
                    'com.synopsys.arc.jenkinsci.plugins.jobrestrictions.jobs.JobRestrictionProperty',
                    'hudson.plugins.copyartifact.CopyArtifactPermissionProperty']
            }
            def restriction = props.appendNode('com.synopsys.arc.jenkinsci.plugins.jobrestrictions.jobs.JobRestrictionProperty').appendNode('config')
            def upstream = restriction.appendNode('upstreamCauseRestriction')
            def regex = upstream.appendNode('jobRestriction', [class: 'com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.job.RegexNameRestriction'])
            regex.appendNode('regexExpression', allowedParents)
            regex.appendNode('checkShortName', false)
            upstream.appendNode('skipCheckForMissingInfo', false)
            restriction.appendNode('userIdCauseRestriction').appendNode('prohibitManualLaunch', true)
            def copyNames = props.appendNode('hudson.plugins.copyartifact.CopyArtifactPermissionProperty').appendNode('projectNameList')
            parentNames.each { copyNames.appendNode('string', it) }
            def definitions = props.'hudson.model.ParametersDefinitionProperty'[0].parameterDefinitions[0]
            ['CI_PARENT_JOB', 'CI_PARENT_BUILD', 'CI_DEVICE_RESOURCE'].each { name ->
                def parameter = definitions.appendNode('hudson.model.StringParameterDefinition')
                parameter.appendNode('name', name)
                parameter.appendNode('defaultValue', '')
                parameter.appendNode('description', 'Internal dispatcher field.')
            }
            def mirrorParameter = definitions.appendNode('hudson.model.BooleanParameterDefinition')
            mirrorParameter.appendNode('name', 'DONT_PUSH_ON_GITHUB')
            mirrorParameter.appendNode('defaultValue', false)

            // Keep release actions only on the original Freestyle release job.
            project.buildWrappers[0].children().removeAll { it.name() == 'hudson.plugins.release.ReleaseWrapper' }
            project.publishers[0].children().removeAll { it.name() == 'jenkins.plugins.slack.SlackNotifier' }
            def environment = props.EnvInjectJobProperty[0].info[0]
            def repoVariables = 'MMT_ANDROID_SCM_URL=ssh://git@git.ib-ci.com:7999/mml/infobip-mobile-messaging-android.git\nMMT_ANDROID_MIRROR_URL=git@github.com:infobip/mobile-messaging-sdk-android.git'
            environment.propertiesContent[0].setValue(environment.propertiesContent.text() + '\n' + repoVariables)
            def remotes = project.scm[0].userRemoteConfigs[0].children()
            assert remotes.size() == 2: 'Android template repository list changed'
            remotes[0].url[0].setValue('${MMT_ANDROID_SCM_URL}')
            remotes[1].url[0].setValue('${MMT_ANDROID_MIRROR_URL}')
            if (!project.scm[0].extensions[0].children().any { it.name().endsWith('.IgnoreNotifyCommit') }) {
                project.scm[0].extensions[0].appendNode('hudson.plugins.git.extensions.impl.IgnoreNotifyCommit')
            }

            def shell = project.builders[0].'hudson.tasks.Shell'.find { it.command.text().contains('AVD_NAME="android37"') }
            assert shell != null: 'Android template emulator shell changed'
            def guard = '''set -e
case "${CI_PARENT_JOB:-}" in
  Team_Mobile_Messaging/infobip-mobile-messaging-android|Team_Mobile_Messaging/infobip-mobile-messaging-android-dispatcher) ;;
  *) echo 'Use the native Android dispatcher'; exit 1 ;;
esac
[ -n "${CI_PARENT_BUILD:-}" ] || { echo 'Missing dispatcher build identity'; exit 1; }
[ "$NODE_NAME" = '@HOST@' ] || { echo 'Wrong worker agent'; exit 1; }
[ "$CI_DEVICE_RESOURCE" = '@RESOURCE@' ] || { echo 'Wrong device resource'; exit 1; }
cd "$WORKSPACE"
printf '%s\\n' '/ci-artifacts/' >> .git/info/exclude
mkdir -p ci-artifacts
python3 - <<'PY'
import json, os, pathlib
pathlib.Path('ci-artifacts/dispatch.json').write_text(json.dumps({
    'node': os.environ['NODE_NAME'],
    'resource': os.environ['CI_DEVICE_RESOURCE'],
    'parent': os.environ['CI_PARENT_BUILD'],
    'commit': os.environ['GIT_COMMIT'],
    'branch': os.environ['BRANCH_NAME_TO_BUILD']
}, indent=2))
PY
'''.replace('@HOST@', host).replace('@RESOURCE@', resource)
            shell.command[0].setValue(shell.command.text().replace('#!/bin/bash\n', '#!/bin/bash\n' + guard))
            def publishers = project.publishers[0]
            def archiver = publishers.appendNode('hudson.tasks.ArtifactArchiver')
            archiver.appendNode('artifacts', artifactPattern)
            archiver.appendNode('allowEmptyArchive', false)
            archiver.appendNode('onlyIfSuccessful', false)
            publishers.children().remove(archiver)
            publishers.children().add(0, archiver)
            publishers.'hudson.tasks.junit.JUnitResultArchiver'[0].allowEmptyResults[0].setValue(false)
            def flexible = publishers.children().find { it.name().endsWith('.FlexiblePublisher') }
            def condition = flexible.depthFirst().find { it.name() == 'condition' && it.attribute('class') == 'org.jenkins_ci.plugins.run_condition.core.ExpressionCondition' }
            assert condition != null: 'Android template mirror condition changed'
            condition.expression[0].setValue('^(master|api25|release_6[.]0[.]0|inbox|release_7[.]0[.]0|in-app-release-candidate|release_14[.]7[.]0):false$')
            condition.label[0].setValue('${BRANCH_NAME_TO_BUILD}:${DONT_PUSH_ON_GITHUB}')
        }
    }
}

freeStyleJob('Team_Mobile_Messaging/native-android-workers/push-trigger') {
    disabled(true)
    concurrentBuild(false)
    label('ci-zg-mac-mini-02 || ci-zg-mac-mini-03')
    description('Compatibility relay for the existing Infobip hook; forwards parameters and exits without waiting.')
    logRotator { daysToKeep(14); numToKeep(30) }
    parameters { stringParam('BRANCH_NAME_TO_BUILD', 'master') }
    scm {
        git {
            remote {
                url('ssh://git@git.ib-ci.com:7999/mml/infobip-mobile-messaging-android.git')
                credentials('006c5191-7380-4eb1-bdbe-82a6f6d98ec2')
            }
            branch('origin/${BRANCH_NAME_TO_BUILD}')
        }
    }
    wrappers { timestamps() }
    steps {
        downstreamParameterized {
            trigger('/Team_Mobile_Messaging/infobip-mobile-messaging-android') {
                parameters { currentBuild() }
                // Never wait for the dispatcher while occupying a Mac executor.
            }
        }
    }
}
