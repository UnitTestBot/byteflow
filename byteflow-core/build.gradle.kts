plugins {
    `maven-publish`
}

dependencies {
    api(Libs.jacodb_api)
    api(Libs.jacodb_core)
    api(Libs.jacodb_analysis)
    implementation(Libs.kotlinx_coroutines_core)

    implementation(Libs.kotlin_logging)
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
