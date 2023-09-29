import com.soywiz.korlibs.modules.*
import org.gradle.api.tasks.testing.logging.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
    }
    dependencies {
        classpath("com.gradle.publish:plugin-publish-plugin:1.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

// @TODO: If we set apply = false, then it doesn't work. But this way we are applying the plugins also for the root project, and that's not what we want
plugins {
    publishing
    `maven-publish`
    kotlin("jvm") version libs.versions.kotlin
    signing
}


//plugins {
//    kotlin("gradle-plugin") apply false
//}

val forcedVersion = System.getenv("FORCED_VERSION")

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
    }
}

subprojects {
    apply(plugin = "java-gradle-plugin")
    apply(plugin = "com.gradle.plugin-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    kotlin {
        jvmToolchain(11) // Target version of generated JVM bytecode. See 7️⃣
    }

    tasks
        .withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java)
        .configureEach {
            compilerOptions.apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7)
            compilerOptions.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7)
        }

    if (forcedVersion != null) {
        version = forcedVersion?.replace("refs/tags/v", "")?.replace("v", "") ?: project.version
    }

    val signingSecretKeyRingFile = System.getenv("ORG_GRADLE_PROJECT_signingSecretKeyRingFile") ?: project.findProperty("signing.secretKeyRingFile")?.toString()

// gpg --armor --export-secret-keys foobar@example.com | awk 'NR == 1 { print "signing.signingKey=" } 1' ORS='\\n'
    val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey") ?: project.findProperty("signing.signingKey")?.toString()
    val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingPassword") ?: project.findProperty("signing.password")?.toString()

    if (signingSecretKeyRingFile != null || signingKey != null) {
        apply(plugin = "signing")
        signing {
            setRequired(provider { !project.version.toString().endsWith("-SNAPSHOT") })
            if (signingKey != null) {
                useInMemoryPgpKeys(signingKey, signingPassword)
            }
            sign(publishing.publications)
        }
    }

    dependencies {
        testImplementation(libs.junit)
        testImplementation("org.jetbrains.kotlin:kotlin-test:${libs.versions.kotlin.get()}")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}")
    }

    val sonatypePublishUser = (System.getenv("SONATYPE_USERNAME") ?: rootProject.findProperty("SONATYPE_USERNAME")?.toString() ?: project.findProperty("sonatypeUsername")?.toString())
    val sonatypePublishPassword = (System.getenv("SONATYPE_PASSWORD") ?: rootProject.findProperty("SONATYPE_PASSWORD")?.toString() ?: project.findProperty("sonatypePassword")?.toString())

    if (sonatypePublishUser == null || sonatypePublishPassword == null) {
        println("Required sonatypeUsername and sonatypePassword in ~/.gradle/gradle.properties")
    }

    tasks {
        val sourcesJar by creating(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.main.map { it.allSource })
        }

        val javadocJar by creating(Jar::class) {
            archiveClassifier.set("javadoc")
        }

            test {
                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                    //events 'started', 'passed', 'skipped', 'failed'
                    events = setOf(TestLogEvent.FAILED)
                    showStandardStreams = true
                }
            }

    }

    publishing {
        repositories {
            if (sonatypePublishUser != null && sonatypePublishPassword != null) {
                val mvn = maven {
                    credentials {
                        username = sonatypePublishUser
                        password = sonatypePublishPassword
                    }
                    val stagedRepositoryIdRef = File(rootProject.buildDir, "stagedRepositoryId.ref")
                    if (stagedRepositoryIdRef.exists()) {
                        url = uri("https://oss.sonatype.org/service/local/staging/deployByRepositoryId/${stagedRepositoryIdRef.readText().trim()}/")
                    } else if (version.toString().contains("-SNAPSHOT")) {
                        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                    } else {
                        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    }
                    println("Repository...$url")
                }
            }
        }
        publications {
            val maven by creating(MavenPublication::class) {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])
            }
        }
        afterEvaluate {
            publications.withType(MavenPublication::class) {
                pom {
                    name.set(project.name)
                    description.set(project.property("project.description").toString())
                    url.set(project.property("project.scm.url").toString())
                    developers {
                        developer {
                            id.set(project.property("project.author.id").toString())
                            name.set(project.property("project.author.name").toString())
                            email.set(project.property("project.author.email").toString())
                        }
                    }
                    licenses {
                        license {
                            name.set(project.property("project.license.name").toString())
                            url.set(project.property("project.license.url").toString())
                        }
                    }
                    scm {
                        url.set(project.property("project.scm.url").toString())
                    }
                }
            }
        }
    }
}

rootProject.configureMavenCentralRelease()
