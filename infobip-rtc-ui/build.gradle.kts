import com.android.build.api.dsl.LibraryExtension

val mmKotlinVersion = providers.gradleProperty("mm_kotlinVersion").get()
val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
val mmAppCompatVersion = providers.gradleProperty("mm_appCompatVersion").get()
val mmMaterialVersion = providers.gradleProperty("mm_materialVersion").get()
val mmJunitVersion = providers.gradleProperty("mm_junitVersion").get()
val mmAndroidJunitVersion = providers.gradleProperty("mm_androidJunitVersion").get()
val mmCoreKtxVersion = providers.gradleProperty("mm_coreKtxVersion").get()
val mmLifecycleVersion = providers.gradleProperty("mm_lifecycleVersion").get()
val mmCoroutinesVersion = providers.gradleProperty("mm_coroutinesVersion").get()
val mmConstraintLayoutVersion = providers.gradleProperty("mm_constraintLayoutVersion").get()
val mmDesugarJdkVersion = providers.gradleProperty("mm_desugarJdkVersion").get()

buildscript {
    val mmGradlePluginVersion = project.providers.gradleProperty("mm_gradlePluginVersion").get()
    val mmKotlinVersion = project.providers.gradleProperty("mm_kotlinVersion").get()

    repositories {
        google()
        mavenCentral()
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
    namespace = "com.infobip.webrtc.ui"
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
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        targetSdk = 37
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

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$mmKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$mmCoroutinesVersion")

    implementation("androidx.core:core-ktx:$mmCoreKtxVersion")
    implementation("androidx.constraintlayout:constraintlayout:$mmConstraintLayoutVersion")
    implementation("androidx.appcompat:appcompat:$mmAppCompatVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$mmLifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$mmLifecycleVersion")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("com.google.android.material:material:$mmMaterialVersion")

    api("com.infobip:infobip-rtc:2.5.40") {
        isTransitive = true
    }

    testImplementation("junit:junit:$mmJunitVersion")

    androidTestImplementation("androidx.test.ext:junit:$mmAndroidJunitVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:$mmDesugarJdkVersion")
}

extra["PUBLISH_ARTIFACT_ID"] = "infobip-rtc-ui"
apply(from = "${rootProject.projectDir}/publish-mavencentral.gradle.kts")
