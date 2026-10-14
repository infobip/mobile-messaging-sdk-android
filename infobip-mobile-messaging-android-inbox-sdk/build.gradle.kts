import com.android.build.api.dsl.LibraryExtension

val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
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
    namespace = "org.infobip.mobile.messaging.inbox"
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
        // Preserve the Groovy DSL value: library defaultConfig.versionName was unset.
        buildConfigField("String", "VERSION_NAME", "\"null\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
        multiDexEnabled = false
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation(project(":infobip-mobile-messaging-android-sdk"))
    implementation(project(":infobip-mobile-messaging-api-java"))

    androidTestImplementation(project(":infobip-mobile-messaging-android-test"))
    androidTestImplementation("androidx.test.ext:junit:$mmAndroidJunitVersion")
    androidTestImplementation("junit:junit:$mmJunitVersion")
    androidTestImplementation("org.mockito:mockito-core:$mmMockitoCoreVersion")
}

extra["PUBLISH_ARTIFACT_ID"] = "infobip-mobile-messaging-android-inbox-sdk"
apply(from = "${rootProject.projectDir}/publish-mavencentral.gradle.kts")
apply(from = "${rootProject.projectDir}/jacoco.gradle.kts")
