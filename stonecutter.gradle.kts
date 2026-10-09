plugins {
    id("dev.kikugie.stonecutter")
    kotlin("jvm") version "2.4.21" apply false
    // Loom variants picked per version by loom-back-compat (see build.gradle.kts)
    id("net.fabricmc.fabric-loom-remap") version "1.18.3" apply false
    id("net.fabricmc.fabric-loom") version "1.18.3" apply false
}

// The version the sources in src/ are currently written for.
// Switch with the "Set active project to ..." Gradle tasks.
stonecutter active "26.3"
