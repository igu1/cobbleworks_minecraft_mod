plugins {
    id("net.neoforged.moddev")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

// Minecraft 26.1.x ships Java 25 to end users, so mods target Java 25.
java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

neoForge {
    version = property("deps.neo_loader") as String
    validateAccessTransformers = true

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("run")
            server()
        }
        register("gameTestServer") {
            gameDirectory = rootProject.file("run-gametest")
            type = "gameTestServer"
            systemProperty("cobbleworks.testing", "true")
        }
        register("clientData") {
            gameDirectory = rootProject.file("run")
            clientData()
            programArguments.addAll("--mod", property("mod.id") as String, "--all",
                    "--output", rootProject.file("src/generated/resources").absolutePath,
                    "--existing", rootProject.file("src/main/resources").absolutePath)
        }
    }
}

// Datagen output (translations, recipes, loot tables) must ship in the jar.
sourceSets.main {
    resources.srcDir("src/generated/resources")
}

tasks {
    val regressionTest = register<JavaExec>("regressionTest") {
        group = "verification"
        description = "Checks redstone, comparator and saved-progress rules"
        dependsOn("testClasses")
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("me.ez.cobbleworks.common.GeneratorRulesTest")
        javaLauncher.set(project.extensions.getByType<org.gradle.jvm.toolchain.JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
    }
    named("check") { dependsOn(regressionTest) }
    named<Test>("test") { failOnNoDiscoveredTests = false }
    // Stonecutter must process the sources before Minecraft artifacts are built.
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // At runtime (from a jar) ${file.jarVersion} resolves to Implementation-Version.
    // In a dev run FML reads the manifest from the resources folder instead, so
    // processResources writes one; otherwise the Mod List shows "0.0NONE".
    jar {
        manifest {
            attributes("Implementation-Version" to project.version)
        }
    }

    named<ProcessResources>("processResources") {
        val manifestFile = layout.buildDirectory.file("resources/main/META-INF/MANIFEST.MF")
        val ver = project.version.toString()
        doLast {
            val f = manifestFile.get().asFile
            f.parentFile.mkdirs()
            f.writeText("Manifest-Version: 1.0\nImplementation-Version: $ver\n")
        }
    }

    // The runs read resources from the projects folder, so make sure processResources
    // runs before they start.
    listOf("clientData", "client", "server", "gameTestServer").forEach { runName ->
        named("prepare${runName.replaceFirstChar { it.uppercase() }}Run") {
            dependsOn("processResources")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the NeoForge 26.1.2 jar and copies it into builds/26.1.2-neoforge/"
        from(layout.buildDirectory.dir("libs")) {
            include("*.jar")
            exclude("*-sources.jar", "*-dev.jar")
        }
        into(rootProject.file("builds/26.1.2-neoforge"))
        dependsOn("build")
    }
}
