val mmJunitVersion = providers.gradleProperty("mm_junitVersion").get()
val mmMockitoCoreVersion = providers.gradleProperty("mm_mockitoCoreVersion").get()

plugins {
    id("java-library")
    id("maven-publish")
    id("idea")
    id("io.freefair.lombok") version "9.5.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["java"])
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            maybeCreate<MavenPublication>("maven").pom {
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    project.configurations.getByName("implementation").allDependencies.forEach {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", it.group)
                        dependencyNode.appendNode("artifactId", it.name)
                        dependencyNode.appendNode("version", it.version)
                    }
                }
            }
        }
    }
}

configurations {
    create("provided")
}

sourceSets {
    getByName("main") {
        compileClasspath += configurations.getByName("provided")
    }
    getByName("test") {
        compileClasspath += configurations.getByName("provided")
    }
}

idea {
    module {
        try {
            scopes["PROVIDED"]?.get("plus")?.add(project.configurations.getByName("provided"))
        } catch (e: NullPointerException) {
            println(":${project.name} $e")
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("commons-codec:commons-codec:1.10")

    testImplementation("junit:junit:$mmJunitVersion")
    testImplementation("org.assertj:assertj-core:2.3.0")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    testImplementation("org.mockito:mockito-core:$mmMockitoCoreVersion")
}

sonar {
    properties {
        property("sonar.java.binaries", layout.buildDirectory.get().asFile.resolve("classes/java/main").absolutePath)
        property("sonar.java.test.binaries", layout.buildDirectory.get().asFile.resolve("classes/java/test").absolutePath)
    }
}

extra["PUBLISH_ARTIFACT_ID"] = "infobip-mobile-messaging-api-java"
apply(from = "${rootProject.projectDir}/publish-mavencentral.gradle.kts")
apply(from = "${rootProject.projectDir}/jacoco.gradle.kts")
