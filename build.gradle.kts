import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// This script is applied to every Minecraft version listed in settings.gradle.kts.
// `sc.current` is the version being built; per-version values come from stonecutter.properties.toml.
plugins {
    // Applies the correct loom variant: remapping for <= 1.21.11, plain loom for 26.1+
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm")
    id("maven-publish")
}

version = "${sc.properties.get<String>("mod.version")}+${sc.current.version}"
group = sc.properties.get<String>("mod.group")

base {
    archivesName.set(sc.properties.get<String>("mod.id"))
}

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("copy-chat") {
            sourceSet("main")
            sourceSet("client")
        }
    }

    runConfigs.all {
        // Shares the run directory between versions
        runDirectory = rootProject.file("run")
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang mappings on obfuscated versions; no-op on 26.1+ (shipped unobfuscated).
    loomx.applyMojangMappings()

    // `mod*` configurations also work on 26.1+, loom-back-compat maps them to the plain ones.
    modImplementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${sc.properties.get<String>("deps.fabric_kotlin")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

tasks.withType<ProcessResources>().configureEach {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to sc.properties.get<String>("mod.mc_compat"),
        "loader_version" to sc.properties.get<String>("deps.fabric_loader"),
        "kotlin_loader_version" to sc.properties.get<String>("deps.fabric_kotlin"),
    )
    val mixinJava = "JAVA_${requiredJava.majorVersion}"
    inputs.properties(props)
    inputs.property("mixin_java", mixinJava)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") { expand(props) }
    filesMatching("*.mixins.json") { expand("java" to mixinJava) }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(requiredJava.majorVersion.toInt())
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(requiredJava.majorVersion))
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds the mod jar and copies it to the root `build/libs/{mod version}/`"

    // loomx.modJar is remapJar on obfuscated versions and jar on 26.1+
    from(loomx.modJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${sc.properties.get<String>("mod.version")}"))
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = sc.properties.get<String>("mod.id")
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
