import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

//to generate coverage report call "debugCoverage" and "jacocoTestReport" tasks
plugins.apply("jacoco")

extensions.configure<JacocoPluginExtension> {
    toolVersion = "0.8.10"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

afterEvaluate {
    if (!plugins.hasPlugin("com.android.library")) {
        tasks.named<JacocoReport>("jacocoTestReport") {
            group = "Reporting"
            description = "Generates Jacoco coverage reports."
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
            executionData.from(fileTree(projectDir) {
                include("**/*.exec", "**/*.ec")
            })
        }
        tasks.register("debugCoverage") {
            group = "Verification"
            description = "Runs Jacoco coverage reports for the debug build."
            dependsOn(tasks.named("jacocoTestReport"))
        }
    } else {
        tasks.register("debugCoverage", JacocoReport::class.java) {
            group = "Reporting"
            description = "Generate Jacoco coverage reports for the debug build."
            // Only the test tasks are required: they produce the execution data and
            // transitively compile the debug classes read below. Inputs are scoped to
            // those tasks' outputs so Gradle's implicit-dependency validation does not
            // require depending on unrelated build tasks.
            dependsOn(tasks.matching { it.name == "connectedDebugAndroidTest" || it.name == "testDebugUnitTest" })
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
            val excludes = listOf(
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "android/**/*.*",
                "androidx/**/*.*",
                "**/*\$ViewInjector*.*",
                "**/*Dagger*.*",
                "**/*MembersInjector*.*",
                "**/*_Factory.*",
                "**/*_Provide*Factory*.*",
                "**/*_ViewBinding*.*",
                "**/AutoValue_*.*",
                "**/R2.class",
                "**/R2\$*.class",
                "**/*Directions\$*",
                "**/*Directions.*",
                "**/*Binding.*"
            )
            // Read compiled classes straight from the compile tasks' outputs so the paths
            // follow AGP/KGP layout changes. Ordering is guaranteed by the test tasks above,
            // which already depend on these compile tasks. Wrapped in a Provider so the
            // directory lookup happens at execution time, after the classes exist.
            val compileTasks = tasks.matching { it.name == "compileDebugJavaWithJavac" || it.name == "compileDebugKotlin" }
            classDirectories.from(provider {
                compileTasks.flatMap { task ->
                    task.outputs.files.filter { it.isDirectory }.map { dir -> fileTree(dir) { exclude(excludes) } }
                }
            })
            val sourceDirs = listOf(
                "${project.projectDir}/src/main/java",
                "${project.projectDir}/src/main/kotlin",
                "${project.projectDir}/src/debug/java",
                "${project.projectDir}/src/debug/kotlin"
            )
            sourceDirectories.from(files(sourceDirs))
            // Execution data produced by testDebugUnitTest (*.exec) and connectedDebugAndroidTest (*.ec).
            executionData.from(fileTree(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest")) {
                include("**/*.exec")
            })
            executionData.from(fileTree(layout.buildDirectory.dir("outputs/code_coverage/debugAndroidTest/connected")) {
                include("**/*.ec")
            })
        }
    }
}
