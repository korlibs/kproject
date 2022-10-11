async function main() {
    console.log("kproject")
    const project = new Project("sample/sample.kproject.json");
    console.log(project)
    console.log(project.buildSettingsGradle())
    //const json = JSON.parse(FS.readText("sample/sample.kproject.json"))
    //GIT.cloneSparse("demo", "https://github.com/korlibs/korge.git", "korge-dragonbones")
    // import os module
}

function error(message) {
    throw new Error(message)
}

class Path {
    static path = require("path")

    static basename(path) {
        return this.path.basename(path)
    }

    static dirname(path) {
        return this.path.dirname(path)
    }

    static resolve(...paths) {
        return this.path.resolve(...paths)
    }
}

class Project {
    static USER_HOME = require("os").homedir();
    static KPROJECT_HOME = `${Project.USER_HOME}/.kproject`

    constructor(pathOrJson, directory = undefined) {
        if (typeof pathOrJson == "string") {
            this.data = JSON.parse(FS.readText(pathOrJson))
            this.jsonDirectory = directory || require("path").dirname(pathOrJson)
        } else {
            this.data = pathOrJson
            this.directory = directory || "."
        }

        this.moduleName = Path.basename(this.data.name || error("Can't find name in json"))
        this.moduleVersion = Path.basename(this.data.version || error("Can't find version in json"))
        this.projectPath = `${Project.KPROJECT_HOME}/${this.moduleName}/${this.moduleVersion}`
    }

    ensureProject() {
        try { FS.fs.mkdirSync(this.projectPath) } catch {}
        const repo = this.data.src.repo || error("Can't find repo in src")
        const path = Path.resolve(this.data.src.path) || error("Can't find path in src")
        const rel = Path.basename(this.data.src.rel || error("Can't find rel in src"))
        FS.mkdirs(this.projectPath)
        if (!FS.exists(this.projectPath)) {
            const git = new GIT(this.projectPath)
            git.cloneSparse(`https://github.com/${repo}.git`, path, rel)
            FS.fs.copyFileSync()
        }
    }

    resolve(info) {
        if (typeof info == "string") {
            return new Project(`${this.jsonDirectory}/${info.replace(/\.kproject\.json$/, '')}.kproject.json`)
        }
        throw new Error(`Unsupported ${info}`)
    }

    buildSettingsGradle() {
        let out = new Indenter()
        for (const dependency of this.data.dependencies) {
            const dependencyProject = this.resolve(dependency);
            dependencyProject.ensureProject()
            out.add(`include ':${dependencyProject.moduleName}'`)

            out.add(`project(':${dependencyProject.moduleName}').projectDir = new File('${dependencyProject.projectPath}')`)
        }
        return out.toString()
    }
}

class Indenter {
    constructor() {
        /**
         * @type {string[]}
         */
        this.lines = [];
    }

    /**
     * @param line {string}
     */
    add(line) {
        this.lines.push(line)
    }

    toString() {
        return this.lines.join("\n")
    }
}

class GIT {
    constructor(clonePath) {
        this.clonePath = clonePath
        this.exec = new Exec(clonePath);
    }

    /**
     * @param repo {string}
     * @param folder {string}
     * @param rel {string}
     */
    cloneSparse(repo, folder, rel) {
        this.exec.execCommand("git", "clone", "--branch", rel, "--depth", "1", "--filter=blob:none", "--sparse", repo, ".")
        this.exec.execCommand("git", "sparse-checkout", "set", folder)
    }

}

class Exec {
    constructor(cwd) {
        this.cwd = cwd;
        try {
            FS.fs.mkdirSync(this.cwd)
        } catch (e) {
        }
    }

    static child = require("child_process")

    /**
     *
     * @param args {string}
     * @returns {string}
     */
    execCommand(...args) {
        return Exec.child.execFileSync(args[0], args.slice(1), {encoding: "utf-8", cwd: this.cwd})
    }
}

class FS {
    static fs = require("fs");

    /**
     * @param path {string}
     * @returns {string}
     */
    static readText(path) {
        return FS.fs.readFileSync(path, {encoding: "utf-8"})
    }

    /**
     *
     * @param path {string}
     * @param text {string}
     */
    static writeText(path, text) {
        FS.fs.writeFileSync(path, text)
    }

    /**
     * @param path {string}
     * @returns {boolean}
     */
    static exists(path) {
        return FS.fs.existsSync(path)
    }

    static mkdirs(path) {
        try { FS.fs.mkdirSync(path, {recursive:true}) } catch {}
    }
}

main();
