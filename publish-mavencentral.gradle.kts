import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension

plugins.apply("maven-publish")
plugins.apply("signing")

//for signing artifacts
extensions.extraProperties["signing.keyId"] = project.findProperty("signing.keyId") ?: System.getenv("SIGNING_KEY_ID")
extensions.extraProperties["signing.password"] = project.findProperty("signing.password") ?: System.getenv("SIGNING_PASSWORD")
extensions.extraProperties["signing.secretKeyRingFile"] = project.findProperty("signing.secretKeyRingFile") ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")

//for connecting to https://oss.sonatype.org/
extensions.extraProperties["ossrhUsername"] = project.findProperty("ossrhUsername") ?: System.getenv("OSSRH_USERNAME")
extensions.extraProperties["ossrhPassword"] = project.findProperty("ossrhPassword") ?: System.getenv("OSSRH_PASSWORD")

//for auto close and release staging repository
extensions.extraProperties["sonatypeStagingProfileId"] = project.findProperty("sonatypeStagingProfileId") ?: System.getenv("SONATYPE_STAGING_PROFILE_ID")

afterEvaluate {
    // Android modules publish the "release" component; the Java-only API module
    // attaches its own artifacts but still needs the shared coordinates and POM metadata.
    val releaseComponent = components.findByName("release")
    extensions.configure<PublishingExtension> {
        publications {
            maybeCreate<MavenPublication>("maven").apply {
                if (releaseComponent != null) {
                    from(releaseComponent)
                }

                groupId = project.group.toString()
                artifactId = extra["PUBLISH_ARTIFACT_ID"].toString()
                version = System.getenv("RELEASE_VERSION") ?: project.version.toString()

                pom {
                    name.set(extra["PUBLISH_ARTIFACT_ID"].toString())
                    description.set("Mobile Messaging SDK for Android")
                    url.set("https://github.com/infobip/mobile-messaging-sdk-android")
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://github.com/infobip/mobile-messaging-sdk-android/blob/master/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("infobip")
                            name.set("Infobip")
                            email.set("support@infobip.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/infobip/mobile-messaging-sdk-android.git")
                        developerConnection.set("scm:git:ssh://github.com/infobip/mobile-messaging-sdk-android.git")
                        url.set("https://github.com/infobip/mobile-messaging-sdk-android")
                    }
                }
            }
        }
    }

    extensions.configure<SigningExtension> {
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}

//    repositories {
//        maven {
//            name = "ossrh-staging-api"
//            url = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
//
//            credentials {
//                username ossrhUsername
//                password ossrhPassword
//            }
//        }
//    }

//Comment out for making local version
