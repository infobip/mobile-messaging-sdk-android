import com.android.build.api.dsl.LibraryExtension

val mmKotlinVersion = providers.gradleProperty("mm_kotlinVersion").get()
val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
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

buildscript {
    val mmGradlePluginVersion = project.providers.gradleProperty("mm_gradlePluginVersion").get()
    val mmKotlinVersion = project.providers.gradleProperty("mm_kotlinVersion").get()

    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$mmGradlePluginVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$mmKotlinVersion")
    }
}

plugins {
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.kotlin.android")
}

extensions.configure<LibraryExtension> {
    namespace = "org.infobip.mobile.messaging.chat"
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
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            multiDexEnabled = true
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

    packaging {
        resources.excludes.add("META-INF/*")
    }
    testOptions {
        targetSdk = 37
    }
}

dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0") {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0") {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }
    }

    implementation(project(":infobip-mobile-messaging-android-sdk"))
    implementation(project(":infobip-mobile-messaging-api-java"))
    api(project(":infobip-mobile-messaging-android-resources"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$mmKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$mmCoroutinesVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$mmLifecycleVersion")

    implementation("androidx.core:core-ktx:$mmCoreKtxVersion")
    implementation("androidx.constraintlayout:constraintlayout:$mmConstraintLayoutVersion")
    implementation("androidx.appcompat:appcompat:$mmAppCompatVersion")
    implementation("androidx.activity:activity-ktx:$mmActivityVersion")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("com.google.android.material:material:$mmMaterialVersion")

    testImplementation(project(":infobip-mobile-messaging-android-test"))
    testImplementation("junit:junit:$mmJunitVersion")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.assertj:assertj-core:3.26.0")

    androidTestImplementation("androidx.test:runner:$mmAndroidTestRunnerVersion")
    androidTestImplementation("androidx.test.ext:junit:$mmAndroidJunitVersion")
    androidTestImplementation("junit:junit:$mmJunitVersion")
    androidTestImplementation("io.mockk:mockk-android:1.13.11")
    androidTestImplementation("androidx.multidex:multidex:$mmMultidexVersion")
}

extra["PUBLISH_ARTIFACT_ID"] = "infobip-mobile-messaging-android-chat-sdk"
apply(from = "${rootProject.projectDir}/publish-mavencentral.gradle.kts")
apply(from = "${rootProject.projectDir}/jacoco.gradle.kts")
