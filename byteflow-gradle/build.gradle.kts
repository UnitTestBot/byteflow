plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization")
    id(Plugins.PluginPublish.id)
    // id(Plugins.Shadow.id)
}

// tasks.shadowJar {
//     archiveClassifier = ""
// }

dependencies {
    api(project(":core"))

    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_serialization_json)
    implementation(Libs.sarif4k)
}

// group = "com.github.UnitTestBot.byteflow"
// version = "0.1.0-SNAPSHOT"

gradlePlugin {
    website = "https://github.com/UnitTestBot/byteflow"
    vcsUrl = "https://github.com/UnitTestBot/byteflow"
    plugins {
        create("byteflowPlugin") {
            id = "io.github.UnitTestBot.byteflow"
            displayName = "Plugin for running ByteFlow analyzer"
            description = "A plugin that runs IFDS analysis"
            implementationClass = "org.byteflow.gradle.ByteFlowPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repository"))
        }
    }
}
