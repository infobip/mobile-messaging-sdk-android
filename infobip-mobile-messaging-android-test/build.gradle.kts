import com.android.build.api.dsl.LibraryExtension

val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
val mmAndroidJunitVersion = providers.gradleProperty("mm_androidJunitVersion").get()
val mmAndroidTestRunnerVersion = providers.gradleProperty("mm_androidTestRunnerVersion").get()
val mmMockitoCoreVersion = providers.gradleProperty("mm_mockitoCoreVersion").get()
val mmJunitVersion = providers.gradleProperty("mm_junitVersion").get()

plugins {
    id("com.android.library")
}


extensions.configure<LibraryExtension> {
    namespace = "org.infobip.mobile.messaging.android"
    buildToolsVersion = mmBuildToolsVersion
    compileSdk = mmCompileSdkVersion

    publishing {
        multipleVariants {
            allVariants()
            withJavadocJar()
            withSourcesJar()
        }
    }

    defaultConfig {
        minSdk = mmMinSdkVersion
        // Preserve the Groovy DSL value: library defaultConfig.versionName was unset.
        buildConfigField("String", "VERSION_NAME", "\"null\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    lint {
        abortOnError = false
        targetSdk = 37
    }
    testOptions {
        targetSdk = 37
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("com.google.code.gson:gson:2.9.0")

    implementation("androidx.test.ext:junit:$mmAndroidJunitVersion")
    implementation("androidx.test:runner:$mmAndroidTestRunnerVersion")
    implementation("com.linkedin.dexmaker:dexmaker-mockito-inline:2.28.3")
    implementation("org.mockito:mockito-core:$mmMockitoCoreVersion")
    implementation("junit:junit:$mmJunitVersion")
    api("org.skyscreamer:jsonassert:1.5.0") {
        exclude(group = "org.json", module = "json")
    }
}
