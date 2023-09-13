plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    `maven-publish`
}

dependencies {
    implementation(Libs.clikt)
    implementation(Libs.jacodb_api)
    implementation(Libs.jacodb_core)
    implementation(Libs.jacodb_analysis)
    implementation(Libs.kotlinx_serialization_json)
    implementation(Libs.kotlinx_coroutines_core)

    implementation(Libs.kotlin_logging)
    implementation(Libs.slf4j_simple)
}

application {
    mainClass = "org.byteflow.CliKt"
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
