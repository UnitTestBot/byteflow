pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.toString() == "byteflow-gradle") {
                useModule("com.github.UnitTestBot.byteflow:gradle:${requested.version}")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven {
            url = uri("../../byteflow-gradle/build/repository")
        }
    }
}
