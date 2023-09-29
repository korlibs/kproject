dependencies {
    implementation(project(":kproject-common"))
}

gradlePlugin {
    website.set("https://github.com/korlibs/kproject")
    vcsUrl.set("https://github.com/korlibs/kproject")
    //tags = ["kproject", "git"]

    plugins {
        val kprojectSettings by creating {
            id = "com.soywiz.kproject.settings"
            displayName = "kproject-settings"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "com.soywiz.kproject.KProjectSettingsPlugin"
        }
    }
}
