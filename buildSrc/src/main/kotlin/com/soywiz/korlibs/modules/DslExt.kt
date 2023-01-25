package com.soywiz.korlibs.modules

import org.gradle.api.*
import org.gradle.api.tasks.*

inline fun <reified T : Task> TaskContainer.createThis(name: String, vararg params: Any, block: T.() -> Unit = {}): T {
    return create(name, T::class.java, *params).apply(block)
}
