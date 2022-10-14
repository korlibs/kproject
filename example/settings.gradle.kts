pluginManagement { repositories {  mavenLocal(); mavenCentral(); google(); gradlePluginPortal()  }  }

plugins {
    id("com.soywiz.kproject.settings") version "0.0.1-SNAPSHOT"
}

kproject("./sample")
kproject("./libs/korge-dragonbones")
//includeKProject("git::korge-dragonbones:korlibs/korge::/korge-dragonbones::v3.2.0")
