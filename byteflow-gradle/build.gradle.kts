plugins {
    id(Plugins.PluginPublish.id)
    // id(Plugins.Shadow.id)
}

dependencies {
    implementation(project(":cli"))

    implementation(Libs.jacodb_api)
    implementation(Libs.jacodb_core)
    implementation(Libs.jacodb_analysis)
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_serialization_json)
}

group = "io.github.UnitTestBot"
version = "0.1.0-SNAPSHOT"

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

// tasks.shadowJar {
//     // archiveBaseName = rootProject.name
//     archiveClassifier = ""
//     // archiveVersion = ""
// }

tasks.register("stuff") {
    doLast {
        println("project '${project.name}' has group = '${project.group}'")
    }
}
