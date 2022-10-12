tasks {
    create("runJvm", JavaExec::class.java) {
        group = "run"
        mainClass.set("MainJvmKt")
        dependsOn("jvmJar")

        //systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        //if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())

        doFirst {
            val jvmCompilation = kotlin.targets.getByName("jvm").compilations
            val mainJvmCompilation = jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
            //println((mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs).files)
            classpath(mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs)
        }
    }
}

