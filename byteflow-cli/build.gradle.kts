plugins {
    kotlin("plugin.serialization")
    application
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

// tasks.shadowJar {
//     archiveBaseName = rootProject.name
//     archiveClassifier = ""
//     archiveVersion = ""
// }
