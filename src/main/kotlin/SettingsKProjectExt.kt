import com.soywiz.kproject.model.*
import org.gradle.api.initialization.*
import java.io.*

/**
 * Example: "git::korge-dragonbones:korlibs/korge::/korge-dragonbones::v3.2.0"
 */
fun Settings.includeKProject(descriptor: String, path: String? = null) {
}

fun Settings.kproject(path: String) {
    KProject.load(File(rootDir, "$path.kproject.json5"), KSet(), root = true).resolve(this)
}
