package com.castle.infrastructure.di

import java.util.concurrent.ConcurrentHashMap

object ServiceRegistry {
    @PublishedApi
    internal val services = ConcurrentHashMap<String, Any>()

    inline fun <reified T : Any> register(instance: T) {
        val type = T::class.qualifiedName
        requireNotNull(type) { "Instance type cannot be null" }

        services.putIfAbsent(type, instance)
    }

    inline fun <reified T : Any> get(): T = services[T::class.qualifiedName] as? T
            ?: error("Service ${T::class.qualifiedName} not registered")

    inline operator fun invoke(block: ServiceRegistry.() -> Unit) = apply(block)

    inline fun <reified T : Any> single(noinline factory: () -> T) = register(factory())
}
