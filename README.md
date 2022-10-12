# kproject

JSON-based source-based kotlin module descriptors

## Define your kotlin multiplatform multi-module projects like this:

### `kproject.json5`

```
{
  name: "korio",
  type: "library",
  version: "3.2.0",
  src: "./src",
  dependencies: [
    "./libs/kds",
    "maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",
  ],
}
```

### `libs/kds.kproject.json5`

```
{
  name: "kds",
  type: "library",
  version: "3.2.0",
  src: "git::korlibs/korge::/kds/src::v3.2.0"
}
```

## No more maven central publishing, distributed sources

With a single `settings.gradle.kts` and `kproject.json5`
files you can reference any version and any project hosted at github or maven repositories
and compile to any supported platform on the fly.

Just put this code in your `settings.gradle.kts`:

```kotlin
val kprojectVersion = "4a7a1c5ea3f961ad8b7863f03d35800c4104df90"
val localFile = file("gradle/$kprojectVersion.settings.gradle.kts")
if (!localFile.exists()) {
    localFile.writeBytes(java.net.URL("https://raw.githubusercontent.com/korlibs/kproject/$kprojectVersion/settings.gradle.kts").readBytes())
}
apply(from = localFile)

rootProject.name = "your-project-name"
```

Run gradle without tasks, and start editing your `kproject.json5` file.
