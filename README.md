# kproject - Liberate your Kotlin projects

YAML-based source-based kotlin module descriptors that runs on top of gradle.

## Define your kotlin multiplatform multi-module projects like this:

### `kproject.yml`

```
name: korio
type: library
version: 3.2.0
src: ./src
dependencies:
- ./libs/kds
- git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db
- maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4

```

## Create kprojects from existing GitHub repositories

Projects can contain `src` folders that are subfolders in git repositories:

### `libs/kds.kproject.yml`

```
name: kds
type: library
version: 3.2.0
src: git::korlibs/korge::/kds/src::v3.2.0
```

## No more maven central publishing, distributed sources

With a single `settings.gradle.kts` and `kproject.yml`
files you can reference any version and any project hosted at github or maven repositories
and compile to any supported platform on the fly.

## Super simple to use:

Just put this code in your `settings.gradle.kts`:

```kotlin
pluginManagement { repositories {  mavenLocal(); mavenCentral(); google(); gradlePluginPortal()  }  }

plugins {
    id("com.soywiz.kproject.settings") version "0.0.1-SNAPSHOT"
}

//kproject("./deps")
```

Run gradle without tasks, and start editing your automatically-generated `kproject.yml` file.

## What solves

Publishing KMP on maven central is a tedious process, that also requires uploading and downloading a lot of files.
It requires a lot of boilerplate and it is not really decentealized. Due to those amount of files that are big in size, publishing new versions is usually delayed
to avoid saturating central, also if the library author publishes one library without a target you need,
you won't be able to support that target, even if the code itself supports it.
With this approach you simplify your projects to something similar to node, haxe or swift.
You can bring sources from other repos, and define dependencies between sources easily.

## How it works

I did an approach in the past to include sourcecode projects withing gradle.
But the problem was that I could not create new modules, but only add new source folders
to other modules. So they could not be compiled in parallel and were being recompiled if the module was dirty.

To fix that, this project wss created.
Kproject injects in the settings.gradle,
in a moment where we can define new modules.
Kproject locates a kproject.yml file,
parses it and resolves all the modules needed
for the project. And then creates gradle modules
based on those; local sources, maven artifcats,
or git-based sources, or remote files
