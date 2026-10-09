pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            // The 1.18-SNAPSHOT marker drifts; pin loom to a concrete build.
            if (requested.id.id == "fabric-loom") {
                useModule("net.fabricmc:fabric-loom:1.18.3")
            }
        }
    }
}
