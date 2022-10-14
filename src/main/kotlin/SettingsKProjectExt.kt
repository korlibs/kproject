import com.soywiz.kproject.model.*
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

fun Settings.kproject(path: String) {
    val file1 = File(rootDir, "$path.kproject.yml")
    val file2 = File(rootDir, "$path/kproject.yml")
    val file = listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find kproject.yml at path $path")
    KProject.load(file, KSet(this), root = true).resolve(this)
}
