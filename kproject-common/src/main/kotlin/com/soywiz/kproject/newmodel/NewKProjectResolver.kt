package com.soywiz.kproject.newmodel

class NewKProjectResolver {
    data class DependencyWithProject(val name: String, val dep: Dependency, val project: NewKProjectModel)

    private val projectsByFile = LinkedHashMap<FileRef, NewKProjectModel>()
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

    fun load(file: FileRef, dep: Dependency = FileRefDependency(file)): NewKProjectModel {
        projectsByFile[file]?.let { return it }

        val project = NewKProjectModel.loadFile(file)
        projectsByFile[file] = project
        val projectName = project.name ?: dep.projectName
        val oldProject = projectsByName[projectName]

        if (oldProject == null || dep > oldProject.dep) {
            //println("projectName:$projectName : $dep")
            val depEx = DependencyWithProject(projectName, dep, project)
            projectsByName[projectName] = depEx
            projectsByDependency[dep] = depEx
            for (dependency in project.dependencies) {
                resolveDependency(dependency)
            }
            return project
        }

        return oldProject.project
    }

    fun resolveDependency(dep: Dependency): NewKProjectModel? {
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
