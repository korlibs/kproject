async function main() {
    console.log("kproject")
    const project = new Project("sample/sample.kproject.json");
    console.log(project)
    console.log(project.buildSettingsGradle())
    //const json = JSON.parse(FS.readText("sample/sample.kproject.json"))
    //GIT.cloneSparse("demo", "https://github.com/korlibs/korge.git", "korge-dragonbones")
}

class Project {
    constructor(pathOrJson, directory = undefined) {
        if (typeof pathOrJson == "string") {
            this.data = JSON.parse(FS.readText(pathOrJson))
            this.directory = directory || require("path").dirname(pathOrJson)
        } else {
            this.data = pathOrJson
            this.directory = directory || "."
        }
    }

    resolve(info) {
        if (typeof info == "string") {
            return new Project(`${this.directory}/${info.replace(/\.kproject\.json$/, '')}.kproject.json`)
        }
        throw new Error(`Unsupported ${info}`)
    }

    buildSettingsGradle() {
        let out = new Indenter()
        for (const dependency of this.data.dependencies) {
            const dependencyProject = this.resolve(dependency);
            const moduleName = dependencyProject.data.name
            out.add(`include ':${moduleName}'`)
            out.add(`project(':${moduleName}').projectDir = new File('../../MyOtherProject/modules/myModule')`)
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
    /**
     * @param clonePath {string}
     * @param repo {string}
     * @param folder {string}
     */
    static cloneSparse(clonePath, repo, folder) {
        const exec = new Exec(clonePath);
        exec.execCommand("git", "clone", "--depth", "1", "--filter=blob:none", "--sparse", repo, ".")
        exec.execCommand("git", "sparse-checkout", "set", folder)
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
}

main();
