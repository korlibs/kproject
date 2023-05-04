import com.soywiz.kproject.model.*
import com.soywiz.kproject.newmodel.*
import org.gradle.api.initialization.*
import java.io.*

/**
 * Example: "git::korge-dragonbones:korlibs/korge::/korge-dragonbones::v3.2.0"
 */
//fun Settings.includeKProject(descriptor: String, path: String? = null) {
//}

//fun Settings.rootKProject() {
//    kproject("./")
//}
fun Settings.kproject(path: String) = kprojectOld(path)
//fun Settings.kproject(path: String) = kprojectNew(path)

fun Settings.kprojectOld(path: String) {
    val file1 = File(rootDir, "$path.kproject.yml")
    val file2 = File(rootDir, "$path/kproject.yml")
    val file = listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find kproject.yml at path $path")
    KProject.load(file, KSet(this), root = true).resolve(this)
}

fun Settings.kprojectNew(path: String) {
    val settings = this
    val file1 = File(rootDir, "$path.kproject.yml")
    val file2 = File(rootDir, "$path/kproject.yml")
    val file = listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find kproject.yml at path $path")
    val results = NewKProjectGradleGenerator(LocalFileRef(rootDir))
        .generate(file.relativeTo(rootDir).path)
    for (result in results) {
        val rname = result.projectName
        val sourceDirectory = (result.projectDir as LocalFileRef).file
        println(":$rname -> $sourceDirectory")
        settings.include(":${rname}")
        settings.project(":${rname}").projectDir = sourceDirectory.relativeTo(rootDir)
    }
}
