package com.soywiz.kproject.newmodel

class NewKProjectResolver {
    class DependencyWithProject(val resolver: NewKProjectResolver, val name: String, val dep: Dependency, val project: NewKProjectModel) {
        val dependencies by lazy {
            project.dependencies.map {
                //println(it)
                resolver.getProjectByDependency(it)
            }
        }

        fun dumpDependenciesToString(): String {
            val out = arrayListOf<String>()
            dumpDependencies { level, name -> out += "${"  ".repeat(level)}$name" }
            return out.joinToString("\n")
        }

        fun dumpDependencies(
            level: Int = 0,
            explored: MutableSet<DependencyWithProject> = mutableSetOf(),
            gen: (level: Int, name: String) -> Unit = { level, name -> println("${"  ".repeat(level)}$name") }
        ) {
            if (this in explored) {
                gen(level, "<recursion detected>")
                return
            }
            explored.add(this)
            gen(level, name)
            for (dependency in dependencies) {
                dependency.dumpDependencies(level + 1, explored, gen)
            }
        }
    }

    private val projectsByFile = LinkedHashMap<FileRef, DependencyWithProject>()
    private val projectsByName = LinkedHashMap<String, DependencyWithProject>()
    private val projectsByDependency = LinkedHashMap<Dependency, DependencyWithProject>()

    fun getProjectNames(): Set<String> = projectsByName.keys
    fun getAllProjects(): Map<String, DependencyWithProject> = projectsByName.toMap()

    fun getProjectByName(name: String): DependencyWithProject =
        projectsByName[name] ?: error("Can't find project $name")

    fun getProjectByDependency(dependency: Dependency): DependencyWithProject {
        resolveDependency(dependency)
        return projectsByDependency[dependency] ?: error("Can't find dependency $dependency")
    }

    fun load(file: FileRef, dep: Dependency = FileRefDependency(file)): DependencyWithProject {
        projectsByFile[file]?.let { return it }

        val project = NewKProjectModel.loadFile(file)
        val projectName = project.name ?: dep.projectName
        val oldProject = projectsByName[projectName]

        if (oldProject == null || dep > oldProject.dep) {
            //println("projectName:$projectName : $dep")
            val depEx = DependencyWithProject(this, projectName, dep, project)
            projectsByName[projectName] = depEx
            projectsByDependency[dep] = depEx
            projectsByFile[file] = depEx
            for (dependency in project.dependencies) {
                resolveDependency(dependency)
            }
            return depEx
        }

        return oldProject
    }

    fun resolveDependency(dep: Dependency): DependencyWithProject? {
        val fileRef = when (dep) {
            is FileRefDependency -> dep.path
            is GitDependency -> dep.file
            is MavenDependency -> null
        }
        return fileRef?.let {
            this@NewKProjectResolver.load(
                when {
                    fileRef.name.endsWith("kproject.yml") -> fileRef
                    else -> fileRef["kproject.yml"]
                }, dep)
        }
    }
}
