plugins {
    `maven-publish`
}

val dist = projectDir.resolve("dist")
val usvmJar = dist.resolve("usvm-jvm-all.jar")
val usvmApiJar = dist.resolve("usvm-api.jar")

val `usvm-approximations` by configurations.creating

val examples by sourceSets.creating {
    java {
        srcDir("src/examples/java")
    }
}

dependencies {
    api(Libs.jacodb_api)
    api(Libs.jacodb_core)
    api(Libs.jacodb_analysis)
    api(Libs.jacodb_approximations)

    implementation(kotlin("reflect"))
    implementation(Libs.kotlinx_coroutines_core)

    implementation(Libs.kotlin_logging)

    implementation(Libs.ksmt_core)
    implementation(Libs.ksmt_z3)

    implementation(files(usvmJar))
    `usvm-approximations`("com.github.UnitTestBot.java-stdlib-approximations:approximations:c992f31c14")

    testImplementation(examples.output)
}

tasks.test {
    maxHeapSize = "2G"
}

tasks.withType<ProcessResources> {
    into("/approximations") {
        `usvm-approximations`.resolvedConfiguration.resolvedArtifacts.forEach {
            from(it.file) {
                rename { "approximations.jar" }
            }
        }

        from(usvmApiJar) {
            rename { "api.jar" }
        }
    }
}

tasks.withType<Jar> {
    into("/") {
        from(zipTree(usvmJar))
    }
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

tasks.withType<Test> {
    maxHeapSize = "2G"
}
