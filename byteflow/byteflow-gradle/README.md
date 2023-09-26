# ByteFlow Gradle Plugin

> This plugin allows for running the ByteFlow analyzer directly from your Gradle project.

## Usage

See [examples](../examples).

### Simple example

Project structure:

```
src/
    main/
        java/
            com.example/
                NpeExamples.java
build.gradle.kts
```

`build.gradle.kts`:

```
plugins {
    id("io.github.UnitTestBot.byteflow") version "0.1.0-SNAPSHOT"
}

// Repositories for plugin dependencies:
buildscript {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// ByteFlow plugin extension allows for customizing the analyzer:
byteflow {
    configFile = layout.projectDirectory.file("configs/config.json")
    startClasses = listOf("com.example.NpeExamples")
    classpath = sourceSets["main"].runtimeClasspath.asPath
}

// ByteFlow provides 'runAnalyzer' task, which requires the classes to analyze.
// You can either run the analyzer as 'gradle compileJava runAnalyzer',
// or simply depend on 'compileJava' task as follows:
tasks.runAnalyzer {
    dependsOn(tasks.compileJava)
}
```

### Complex example

// TODO
