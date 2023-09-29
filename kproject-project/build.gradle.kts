//def isJava8or9 = System.getProperty("java.version").startsWith("1.8") || System.getProperty("java.version").startsWith("9")

dependencies {
    implementation(project(":kproject-common"))
    implementation(libs.kotlin.gradle)
    implementation(libs.android.build.gradle)
}

gradlePlugin {
    website.set("https://github.com/korlibs/kproject")
    vcsUrl.set("https://github.com/korlibs/kproject")
    //tags = ["kproject", "git"]

    plugins {
        val kproject by creating {
            id = "com.soywiz.kproject"
            displayName = "kproject"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectPlugin"
        }
        val kprojectRoot by creating {
            id = "com.soywiz.kproject.root"
            displayName = "kproject"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectRootPlugin"
        }
    }
}
