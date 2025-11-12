import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libraries.plugins.kotlin.jvm)
    alias(libraries.plugins.google.protobuf)

    application
}

group = "com.castle"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libraries.bundles.vertx)
    implementation(libraries.bundles.grpc)
    implementation(libraries.bundles.protobuf)
    implementation(libraries.coroutines.core)
    implementation(libraries.paseto4j)
    implementation(libraries.bouncy.castle4j)
    implementation(libraries.jackson.kotlin)

    testImplementation(libraries.bundles.testing)
}

val jdkVersion: Int = 21

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(jdkVersion)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.3"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libraries.versions.grpc.protobuf.get()}"
        }

        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libraries.versions.grpc.kotlin.get()}:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
                create("grpckt")
            }

            task.builtins {
                create("kotlin")
            }
        }
    }
}

application {
    mainClass.set("com.example.ApplicationKt")

    applicationDefaultJvmArgs = listOf(
        "-Dkotlinx.coroutines.virtual.threads=true",
        "-Dvertx.virtual.threads=true",
        "-Dvertx.application.hooks=com.example.application.launcher.CustomHooks"
    )
}

kotlin {
    jvmToolchain(jdkVersion)
}

tasks.withType<KotlinCompile> {
    dependsOn("generateProto")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.example.ApplicationKt",
            "Vertx-Application-Hooks" to "com.example.application.launcher.CustomHooks"
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}