import com.android.build.api.dsl.LibraryExtension

val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
val mmFirebaseMessagingVersion = providers.gradleProperty("mm_firebaseMessagingVersion").get()
val mmAndroidJunitVersion = providers.gradleProperty("mm_androidJunitVersion").get()
val mmJunitVersion = providers.gradleProperty("mm_junitVersion").get()
val mmMockitoCoreVersion = providers.gradleProperty("mm_mockitoCoreVersion").get()

buildscript {
    val mmGradlePluginVersion = project.providers.gradleProperty("mm_gradlePluginVersion").get()

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$mmGradlePluginVersion")
    }
}

plugins {
    id("com.android.library")
    id("maven-publish")
    id("idea")
    id("org.jetbrains.kotlin.android")
}

extensions.configure<LibraryExtension> {
    namespace = "org.infobip.mobile.messaging"
    buildToolsVersion = mmBuildToolsVersion
    compileSdk = mmCompileSdkVersion

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }

    defaultConfig {
        minSdk = mmMinSdkVersion
        buildConfigField("String", "MM_SDK_VERSION", "\"${rootProject.extra["mm_version"]}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        abortOnError = false
        targetSdk = 37
    }
    testOptions {
        targetSdk = 37
    }
}

dependencies {
    api(project(":infobip-mobile-messaging-api-java"))
    api(project(":infobip-mobile-messaging-android-resources"))
    api("com.google.firebase:firebase-messaging:$mmFirebaseMessagingVersion")
    api("com.google.android.gms:play-services-base:18.2.0")
    api("androidx.legacy:legacy-support-v4:1.0.0")
    api("androidx.appcompat:appcompat:1.3.1")

    androidTestImplementation(project(":infobip-mobile-messaging-android-test"))
    androidTestImplementation("androidx.test.ext:junit:$mmAndroidJunitVersion")
    androidTestImplementation("junit:junit:$mmJunitVersion")
    androidTestImplementation("org.mockito:mockito-core:$mmMockitoCoreVersion")
    androidTestImplementation("org.skyscreamer:jsonassert:1.5.0") {
        exclude(group = "org.json", module = "json")
    }

    testImplementation(project(":infobip-mobile-messaging-android-test"))
    testImplementation("junit:junit:$mmJunitVersion")

    implementation("androidx.work:work-runtime:2.8.1")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("androidx.cardview:cardview:1.0.0")
}

extra["PUBLISH_ARTIFACT_ID"] = "infobip-mobile-messaging-android-sdk"
apply(from = "${rootProject.projectDir}/publish-mavencentral.gradle.kts")
apply(from = "${rootProject.projectDir}/jacoco.gradle.kts")
