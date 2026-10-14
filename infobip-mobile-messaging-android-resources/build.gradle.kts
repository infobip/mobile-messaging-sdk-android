import com.android.build.api.dsl.LibraryExtension

val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()

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
    namespace = "org.infobip.mobile.messaging.resources"
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
        consumerProguardFiles("proguard-rules.pro")
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

extra["PUBLISH_ARTIFACT_ID"] = "infobip-mobile-messaging-android-resources"
apply(from = "${rootProject.projectDir}/publish-mavencentral.gradle.kts")
