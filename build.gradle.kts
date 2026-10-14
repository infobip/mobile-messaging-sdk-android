import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

buildscript {
    val mmGradlePluginVersion = project.providers.gradleProperty("mm_gradlePluginVersion").get()
    val mmDesugarJdkVersion = project.providers.gradleProperty("mm_desugarJdkVersion").get()

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$mmGradlePluginVersion")
        classpath("com.android.tools:desugar_jdk_libs:$mmDesugarJdkVersion")
    }
}

plugins {
    id("net.researchgate.release") version "3.1.0"
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jetbrains.kotlin.android") apply false
    id("org.sonarqube") version "7.3.1.8318"
    id("jacoco")
    id("com.diffplug.spotless") version "8.0.0" apply false
}

val mmKotlinVersion = providers.gradleProperty("mm_kotlinVersion").get()
val mmGradlePluginVersion = providers.gradleProperty("mm_gradlePluginVersion").get()
val mmDesugarJdkVersion = providers.gradleProperty("mm_desugarJdkVersion").get()

val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmTargetSdkVersion = providers.gradleProperty("mm_targetSdkVersion").get().toInt()
val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmFirebaseMessagingVersion = providers.gradleProperty("mm_firebaseMessagingVersion").get()
val mmGmsLocationVersion = providers.gradleProperty("mm_gmsLocationVersion").get()
val mmConstraintLayoutVersion = providers.gradleProperty("mm_constraintLayoutVersion").get()
val mmAppCompatVersion = providers.gradleProperty("mm_appCompatVersion").get()
val mmActivityVersion = providers.gradleProperty("mm_activityVersion").get()
val mmMaterialVersion = providers.gradleProperty("mm_materialVersion").get()
val mmCoreKtxVersion = providers.gradleProperty("mm_coreKtxVersion").get()
val mmLifecycleVersion = providers.gradleProperty("mm_lifecycleVersion").get()
val mmJunitVersion = providers.gradleProperty("mm_junitVersion").get()
val mmAndroidJunitVersion = providers.gradleProperty("mm_androidJunitVersion").get()
val mmMockitoCoreVersion = providers.gradleProperty("mm_mockitoCoreVersion").get()
val mmMultidexVersion = providers.gradleProperty("mm_multidexVersion").get()
val mmCoroutinesVersion = providers.gradleProperty("mm_coroutinesVersion").get()
val mmAndroidTestRunnerVersion = providers.gradleProperty("mm_androidTestRunnerVersion").get()

extra["mm_kotlinVersion"] = mmKotlinVersion
extra["mm_gradlePluginVersion"] = mmGradlePluginVersion
extra["mm_desugarJdkVersion"] = mmDesugarJdkVersion
extra["mm_minSdkVersion"] = mmMinSdkVersion
extra["mm_compileSdkVersion"] = mmCompileSdkVersion
extra["mm_targetSdkVersion"] = mmTargetSdkVersion
extra["mm_buildToolsVersion"] = mmBuildToolsVersion
extra["mm_firebaseMessagingVersion"] = mmFirebaseMessagingVersion
extra["mm_gmsLocationVersion"] = mmGmsLocationVersion
extra["mm_constraintLayoutVersion"] = mmConstraintLayoutVersion
extra["mm_appCompatVersion"] = mmAppCompatVersion
extra["mm_activityVersion"] = mmActivityVersion
extra["mm_materialVersion"] = mmMaterialVersion
extra["mm_coreKtxVersion"] = mmCoreKtxVersion
extra["mm_lifecycleVersion"] = mmLifecycleVersion
extra["mm_junitVersion"] = mmJunitVersion
extra["mm_androidJunitVersion"] = mmAndroidJunitVersion
extra["mm_mockitoCoreVersion"] = mmMockitoCoreVersion
extra["mm_multidexVersion"] = mmMultidexVersion
extra["mm_coroutinesVersion"] = mmCoroutinesVersion
extra["mm_androidTestRunnerVersion"] = mmAndroidTestRunnerVersion
extra["mm_version"] = version.toString()

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "com.diffplug.spotless")

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf("com.android.library", "com.android.application").forEach { pluginId ->
        plugins.withId(pluginId) {
            val androidLintReports = tasks.matching {
                it.name.startsWith("lintReport") || it.name.startsWith("lintVitalReport")
            }

            tasks.matching {
                it.name == "sonarResolver" || it.name == "sonar" || it.name == "sonarqube"
            }.configureEach {
                dependsOn(rootProject.project(":infobip-mobile-messaging-api-java").tasks.named("jar"))
                dependsOn(androidLintReports)
            }
        }
    }

    extensions.configure<SpotlessExtension>("spotless") {
        java {
            target("src/**/*.java")
            targetExclude("**/build/**", "**/generated/**", "**/intermediates/**", "**/tmp/**", "**/DebugServer.java")

            if (project.name == "infobip-rtc-ui") {
                licenseHeaderFile(rootProject.file("spotless/copyright-header-rtc.txt"))
            } else {
                licenseHeaderFile(rootProject.file("spotless/copyright-header.txt"))
            }
        }

        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**", "**/generated/**", "**/intermediates/**", "**/tmp/**")

            if (project.name == "infobip-rtc-ui") {
                licenseHeaderFile(rootProject.file("spotless/copyright-header-rtc.txt"))
            } else {
                licenseHeaderFile(rootProject.file("spotless/copyright-header.txt"))
            }
        }

        format("xml") {
            target("src/**/*.xml")
            targetExclude("**/build/**", "**/generated/**", "**/intermediates/**", "**/tmp/**")

            //<[a-zA-Z] matching the beginning of the XML root tag, everything before that will be replaced with the license header
            if (project.name == "infobip-rtc-ui") {
                licenseHeaderFile(rootProject.file("spotless/copyright-header-xml-rtc.txt"), "<[a-zA-Z]")
            } else {
                licenseHeaderFile(rootProject.file("spotless/copyright-header-xml.txt"), "<[a-zA-Z]")
            }
        }
    }

    /**
     * 1. build task → depends on check task
     * 2. check task → depends on spotlessCheck task (and test)
     * 3. spotlessCheck → validates code formatting
     *
     * So when you run ./gradlew build, it will:
     * - Run tests
     * - Run Spotless formatting checks
     * - Build the project
     *
     * If Spotless finds any formatting violations or missing copyright, the build will fail.
     */
    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
}

tasks.matching {
    it.name == "sonar" || it.name == "sonarqube"
}.configureEach {
    dependsOn(rootProject.project(":infobip-mobile-messaging-api-java").tasks.named("jar"))
}

tasks.named("sonarqube") {
    dependsOn(
        subprojects.map { subproject ->
            subproject.tasks.matching {
                it.name == "lint" || it.name == "lintReportRelease"
            }
        }
    )
}

tasks.register("publishAllPublicationsToMavenRepository") {
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { task -> task.name.startsWith("publish") && task.name.endsWith("ToSonatypeRepository") })
    }
}

tasks.register("debugCoverage") {
    group = "Verification"
    description = "Runs debug coverage reports for all modules."
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "debugCoverage" })
    }
}

tasks.register("updateVersionInReadme") {
    doLast {
        val readmeFile = file("README.md")
        val text = readmeFile.readText()
        readmeFile.writeText(
            text.replaceFirst(
                "infobip-mobile-messaging-android-sdk:\\d+\\.\\d+\\.\\S+@aar".toRegex(),
                "infobip-mobile-messaging-android-sdk:$version@aar"
            )
        )
    }
}

tasks.register("updateRtcUiVersionInReadme") {
    doLast {
        val readmeFile = file("infobip-rtc-ui/README.md")
        val text = readmeFile.readText()
        readmeFile.writeText(
            text.replaceFirst(
                "'com.infobip:infobip-rtc-ui:\\d+\\.\\d+\\.\\S+'".toRegex(),
                "'com.infobip:infobip-rtc-ui:$version'"
            )
        )
    }
}

tasks.named("afterReleaseBuild") {
    dependsOn("updateVersionInReadme", "updateRtcUiVersionInReadme")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username = providers.gradleProperty("ossrhUsername").orElse(providers.environmentVariable("OSSRH_USERNAME")).orNull
            password = providers.gradleProperty("ossrhPassword").orElse(providers.environmentVariable("OSSRH_PASSWORD")).orNull
            stagingProfileId.set(
                providers.gradleProperty("sonatypeStagingProfileId")
                    .orElse(providers.environmentVariable("SONATYPE_STAGING_PROFILE_ID"))
            )
        }
    }
    connectTimeout = Duration.ofMinutes(3)
    clientTimeout = Duration.ofMinutes(3)
}

tasks.named("closeAndReleaseSonatypeStagingRepository") {
    mustRunAfter(subprojects.map { it.tasks.named("publish") })
}

tasks.named("afterReleaseBuild") {
    dependsOn(subprojects.map { it.tasks.named("publish") })
    dependsOn("closeAndReleaseSonatypeStagingRepository")
}

release {
    buildTasks = listOf("clean", "build")
    git {
        requireBranch.set("")
        pushToRemote.set("origin")
    }
}

sonar {
    properties {
        property("sonar.host.url", "https://sonar.ib-ci.com")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.branch.name", System.getenv("BRANCH_NAME_TO_BUILD"))
        property("sonar.projectVersion", project.version)
        property("sonar.projectName", "infobip-mobile-messaging-android")
        property("sonar.projectKey", "infobip-mobile-messaging-android")
        property("sonar.androidLint.reportPaths", "${layout.buildDirectory}/reports/lint-results.xml")
        property(
            "sonar.exclusions",
            "" +
                "infobip-mobile-messaging-android-demo/**/*, " +
                "infobip-mobile-messaging-android-cryptor-migration/**/*, " +
                "infobip-mobile-messaging-android-test/**/*, " +
                "infobip-rtc-ui/**/*"
        )
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.junit.reportsPath", "${layout.buildDirectory}/test-results/test/")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "" +
                "${project.projectDir}/infobip-mobile-messaging-android-inbox-sdk/build/reports/jacoco/debugCoverage/debugCoverage.xml, " +
                "${project.projectDir}/infobip-mobile-messaging-android-sdk/build/reports/jacoco/debugCoverage/debugCoverage.xml, " +
                "${project.projectDir}/infobip-mobile-messaging-api-java/build/reports/jacoco/test/jacocoTestReport.xml"
        )
    }
}
