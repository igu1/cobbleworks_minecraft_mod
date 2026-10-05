plugins {
    id("dev.kikugie.stonecutter")
    // Modern NeoForge toolchain (ModDevGradle 2.x).
    id("net.neoforged.moddev") version "2.0.147" apply false
}

// The version whose source is currently checked out in src/.
stonecutter active "26.1.2"
