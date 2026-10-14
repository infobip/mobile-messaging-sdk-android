import com.android.build.api.dsl.ApplicationExtension

val mmBuildToolsVersion = providers.gradleProperty("mm_buildToolsVersion").get()
val mmCompileSdkVersion = providers.gradleProperty("mm_compileSdkVersion").get().toInt()
val mmMinSdkVersion = providers.gradleProperty("mm_minSdkVersion").get().toInt()
val mmTargetSdkVersion = providers.gradleProperty("mm_targetSdkVersion").get().toInt()
val mmAppCompatVersion = providers.gradleProperty("mm_appCompatVersion").get()
val mmConstraintLayoutVersion = providers.gradleProperty("mm_constraintLayoutVersion").get()
val mmMaterialVersion = providers.gradleProperty("mm_materialVersion").get()
val mmAndroidJunitVersion = providers.gradleProperty("mm_androidJunitVersion").get()
val mmJunitVersion = providers.gradleProperty("mm_junitVersion").get()
val mmDesugarJdkVersion = providers.gradleProperty("mm_desugarJdkVersion").get()

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
    }
}

plugins {
    id("com.android.application")
}

extensions.configure<ApplicationExtension> {
    namespace = "org.infobip.mobile.messaging.demo"
    buildToolsVersion = mmBuildToolsVersion
    compileSdk = mmCompileSdkVersion

    defaultConfig {
        minSdk = mmMinSdkVersion
        targetSdk = mmTargetSdkVersion
        versionCode = 1
        versionName = project.version.toString()
        applicationId = "org.infobip.mobile.messaging.demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        resValues = true
    }

    signingConfigs {
        create("release") {
            property(project, "signingKeystore", "KEYSTORE")?.let { storeFile = file(it) }
            storePassword = property(project, "signingKeystorePassword", "KEYSTORE_PASSWORD")
            keyAlias = "alias_name"
            keyPassword = if (project.hasProperty("signingKeystorePassword")) {
                project.property("signingKeystorePassword").toString()
            } else {
                System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        configureEach {
            property(project, "google_app_id", "GOOGLE_APP_ID")?.let { resValue("string", "google_app_id", it) }
            property(project, "infobip_application_code", "INFOBIP_APPLICATION_CODE")?.let { resValue("string", "infobip_application_code", it) }
            property(project, "infobip_api_uri", "INFOBIP_API_URI")?.let { resValue("string", "infobip_api_uri", it) }
            property(project, "infobip_livechat_widget_uri", "INFOBIP_LIVECHAT_WIDGET_URI")?.let { resValue("string", "ib_livechat_widget_uri", it) }
        }

        release {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "default"

    productFlavors {
        create("push") { dimension = "default" }
        create("chat") { dimension = "default" }
        create("chatWithCalls") {
            dimension = "default"
            minSdk = 21
            property(project, "infobip_webrtc_configuration_id", "INFOBIP_WEBRTC_CONFIGURATION_ID")?.let {
                resValue("string", "infobip_webrtc_configuration_id", it)
            }
        }
        create("deeplink") { dimension = "default" }
        create("web") { dimension = "default" }
        create("customInApp") { dimension = "default" }
        create("cryptorMigration") { dimension = "default" }
        create("inbox") { dimension = "default" }
    }

    sourceSets {
        getByName("chatWithCalls") {
            java.srcDirs("src/chatCommon/java")
            kotlin.srcDirs("src/chatCommon/java", "src/chatCommon/kotlin")
            res.srcDirs("src/chatCommon/res")
        }
        getByName("chat") {
            java.srcDirs("src/chatCommon/java")
            kotlin.srcDirs("src/chatCommon/java", "src/chatCommon/kotlin")
            res.srcDirs("src/chatCommon/res")
        }
    }

    lint {
        abortOnError = false
    }
}

tasks.named("sonarResolver") {
    dependsOn(tasks.matching { it.name.startsWith("generate") && it.name.endsWith("ResValues") })
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:$mmConstraintLayoutVersion")
    testImplementation("junit:junit:$mmJunitVersion")
    androidTestImplementation("androidx.test.ext:junit:$mmAndroidJunitVersion")

    implementation(project(":infobip-mobile-messaging-android-chat-sdk"))
    implementation(project(":infobip-mobile-messaging-android-resources"))
    implementation(project(":infobip-mobile-messaging-android-sdk"))
    implementation(project(":infobip-mobile-messaging-api-java"))
    implementation(project(":infobip-mobile-messaging-android-inbox-sdk"))

    implementation("androidx.appcompat:appcompat:$mmAppCompatVersion")
    implementation("com.google.android.material:material:$mmMaterialVersion")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.nimbusds:nimbus-jose-jwt:9.0.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:$mmDesugarJdkVersion")

    //To add WebRTC use build flavour "chatWithCalls"
    add("chatWithCallsImplementation", project(":infobip-rtc-ui"))

    //To add cryptor use build flavour "cryptorMigration"
    add("cryptorMigrationImplementation", project(":infobip-mobile-messaging-android-cryptor-migration"))
}

fun property(project: Project, projectKey: String, environmentKey: String): String? {
    return if (project.hasProperty(projectKey)) {
        project.property(projectKey).toString()
    } else {
        System.getenv(environmentKey)
    }
}

tasks.register("uncommentGoogleServices") {
    doLast {
        val buildFile = project.file("build.gradle.kts")
        val text = buildFile.readText()
        buildFile.writeText(
            text.replaceFirst(
                "//apply\\(plugin = \\\"com.google.gms.google-services\\\"\\)".toRegex(),
                "apply(plugin = \\\"com.google.gms.google-services\\\")"
            )
        )
    }
}

//It's for usage of google-services.json file, if you are using strings.xml to provide Firebase data, comment next line
//apply(plugin = "com.google.gms.google-services")
