import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.javamodularity.moduleplugin.extensions.TestModuleOptions
import org.gradle.internal.os.OperatingSystem


plugins {
    val kotlinVersion = "2.2.21"
    application

    jacoco
    idea
    antlr
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.sonarqube") version "7.2.2.6593"
    id("com.github.ben-manes.versions") version "0.53.0"

    id("org.openjfx.javafxplugin") version "0.1.0"

    id("org.javamodularity.moduleplugin") version "2.0.0"
    id("org.beryx.jlink") version "3.1.5"

    id("com.palantir.git-version") version "4.2.0"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/spekframework/spek/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.atlassian.com/content/repositories/atlassian-public/")
}

// -SNAPSHOT is added if the release task is not set
// jpackge for windows needs an 2 digit version, at least
val upcomingVersion = "4.0"
val archivesBaseName = "STT"

version = upcomingVersion

application {
    mainModule.set("org.stt")
    mainClass.set("org.stt.StartWithJFX")
    // add-opens, so we can access the file decoration-warning.png within this module
    applicationDefaultJvmArgs =
        listOf("--add-opens=org.controlsfx.controls/impl.org.controlsfx.control.validation=org.stt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

    java {
        // workaround, to make kapt created classes available to java module source set
        sourceSets {
            main {
                java {
                    srcDir(layout.buildDirectory.dir("generated/source/kapt/main"))
                }
            }
        }
    }
val spek_version = "2.0.4"

dependencies {
    val daggerVersion = "2.58"
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    implementation("org.fxmisc.richtext:richtextfx:0.11.7") {
        exclude(group = "org.openjfx")
    }
    implementation("org.yaml:snakeyaml:2.5")
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    implementation("net.engio:mbassador:1.3.2")
    implementation("org.controlsfx:controlsfx:11.2.3")
    implementation("com.jsoniter:jsoniter:0.9.23")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("commons-io:commons-io:2.21.0")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("junit:junit-dep:4.11")
}

javafx {
    version = "21.0.4"
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}

sonar {
    properties {
        property("sonar.projectkey", "org.stt:stt")
        property("sonar.projectName", "SimpleTimeTracking")
    }
}

distributions.getByName("main") {
    contents {
        include("**/STT*")
    }
}

tasks.compileJava {
    // workaround, to make kapt created classes available to java module source set
    sourceSets {
        main {
                java {
                    srcDir(layout.buildDirectory.dir("generated/source/kapt/main").get().asFile)
                }
        }
    }
}

tasks.test {
    extensions.configure(TestModuleOptions::class) {
        // disable java-module path for tests
        runOnClasspath = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask> {
    dependsOn(tasks.withType<AntlrTask>())
}
// provided by plugin: com.palantir.git-version
val gitVersion: groovy.lang.Closure<String> by project.extra
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra

// Capture values at configuration time to avoid Task.project access at execution time
val resolvedAppVersion: String = project.version.toString()
val resolvedGitHash: String = versionDetails().gitHash

tasks.withType<ProcessResources> {
    filesMatching("version.info") {
        filter<ReplaceTokens>(
            "tokens" to mapOf(
                "app.version" to resolvedAppVersion,
                "app.hash" to resolvedGitHash
            )
        )
    }
    doLast {
        println("Written tokes into file version.info (version=${resolvedAppVersion})")
    }
}

tasks.register("dist") {
    dependsOn("jpackage", "jlinkZip")
}

tasks.register("release") {
    dependsOn("dist")
    doLast {
        println("Built release for ${resolvedAppVersion}")
    }
}

gradle.taskGraph.whenReady {
    if (!hasTask("release")) {
        version = (upcomingVersion as String) + "-SNAPSHOT"
    }
}

tasks.withType<AntlrTask> {
    maxHeapSize = "64m"
    arguments = arguments + "-visitor" + "-long-messages"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget("21"))
    }
}

//tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java).configure {
//    val badVersions = ".*(-rc(-.*)?$|-m.*)".toRegex()
//    rejectVersionIf {
//        candidate.version.toLowerCase().matches(badVersions)
//    }
//}

jlink {
    imageZip.set(layout.buildDirectory.file("dist/zips/stt-${javafx.platform.classifier}-${version}.zip"))
    addOptions("--bind-services", "--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    mergedModule {
        excludeRequires("javafx.graphics", "javafx.controls", "javafx.base")
    }
    forceMerge("kotlin") // see https://stackoverflow.com/questions/74453018/jlink-package-kotlin-in-both-merged-module-and-kotlin-stdlib
    launcher {
        name = "stt"
        jvmArgs =
            application.applicationDefaultJvmArgs.plus(
                listOf(
                    // some classes in the merged-modules (see jlink plugin( need access to javafx modules
                    "--add-reads=simpleTimeTracking.merged.module=javafx.graphics",
                    "--add-reads=simpleTimeTracking.merged.module=javafx.base",
                    "--add-reads=simpleTimeTracking.merged.module=javafx.controls"
                )
            )
    }
    jpackage {
        installerOutputDir = layout.buildDirectory.dir("dist/installer/${javafx.platform.classifier}").get().asFile
        skipInstaller = false
        appVersion = upcomingVersion

        val os = OperatingSystem.current()
        println("Building on ${os.toString()}.")

        if (os.isLinux || os.isUnix) {
            icon = "src/main/resources/Logo.png"
            installerOptions.plus(listOf(
                "--linux-menu-group", "Office",
                "--linux-shortcut"))
        }
        if (os.isWindows) {
            installerType = "exe"
            icon = "src/main/resources/Logo.ico"
            installerOptions.plus(listOf(
                "--win-upgrade-uuid", "0e521a7f-2fe2-4d30-9065-8a6972e11b56",
                "--win-shortcut"))
        }
        if (os.isMacOsX) {
            installerType = "dmg"
            icon = "src/main/resources/Logo.icns"
        }
    }
}

