# kproject - Liberate your Kotlin projects

YAML-based source-based kotlin module descriptors that runs on top of gradle.

## [Full documentation & presentation](http://docs.korge.org/kproject/)

## Define your kotlin multiplatform multi-module projects like this:

### `deps.kproject.yml`

```
plugins:
- serialization
dependencies:
- ./libs/kds
- https://github.com/korlibs/kproject/tree/54f73b01cea9cb2e8368176ac45f2fca948e57db/modules/adder
- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
```

## You can create a kproject that uses other git project as source

### `kproject.yml`

```
src: https://github.com/Quillraven/Fleks/tree/c24925091ced418bf045ba0672734addaab573d8/src
plugins:
- serialization
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
    id("com.soywiz.kproject.settings") version "0.2.1"
}
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
