package com.castle.infrastructure.di

import java.util.concurrent.ConcurrentHashMap

object ServiceRegistry {
    @PublishedApi
    internal val services = ConcurrentHashMap<String, Any>()

    inline fun <reified T : Any> register(instance: T) {
        services[T::class.simpleName ?: ""] = instance
    }

    inline fun <reified T : Any> get(): T = services[T::class.simpleName] as? T
            ?: error("Service ${T::class.simpleName} not registered")

    fun clear() = services.clear()

    inline operator fun invoke(block: ServiceRegistry.() -> Unit) = apply(block)

    inline fun <reified T : Any> single(factory: () -> T) = register(factory())
}
