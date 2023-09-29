dependencies {
    //implementation(gradleApi())
    //implementation(localGroovy())
    //implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation(libs.jgit)
}

val srcgen = File(project.buildDir, "srcgen")
kotlin.sourceSets.maybeCreate("main").kotlin.srcDirs(srcgen)
val KProjectVersionKt = File(srcgen, "KProjectVersion.kt")
val KProjectVersionContent = """
package com.soywiz.kproject.version

object KProjectVersion {
    val VERSION = "${version}"
}
"""
if (!KProjectVersionKt.exists() || KProjectVersionKt.readText() != KProjectVersionContent) {
    srcgen.mkdirs()
    KProjectVersionKt.writeText(KProjectVersionContent)
}
