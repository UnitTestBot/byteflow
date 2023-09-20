plugins {
    kotlin("plugin.serialization")
    application
    `maven-publish`
    // id(Plugins.Shadow.id)
}

dependencies {
    implementation(project(":core"))

    implementation(Libs.clikt)
    implementation(Libs.kotlinx_serialization_json)
    implementation(Libs.sarif4k)
    implementation(Libs.kotlin_logging)
    implementation(Libs.slf4j_simple)
}

application {
    mainClass = "org.byteflow.cli.CliKt"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8")
}

tasks.startScripts {
    applicationName = rootProject.name
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repository"))
        }
    }
}

// tasks.shadowJar {
//     archiveBaseName = rootProject.name
//     archiveClassifier = ""
//     archiveVersion = ""
// }
