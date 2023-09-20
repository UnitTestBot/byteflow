# ByteFlow Nexus

[![CI](https://github.com/UnitTestBot/byteflow/actions/workflows/ci.yml/badge.svg)](https://github.com/UnitTestBot/byteflow/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/UnitTestBot/byteflow.svg)](https://jitpack.io/p/UnitTestBot/byteflow)
[![License](https://img.shields.io/github/license/UnitTestBot/byteflow)](LICENSE)

## Run

### CLI application

You can run the ByteFlow analyzer via command-line interface:

```shell
./gradlew -q :cli:installDist
./byteflow-cli/build/install/cli/bin/byteflow -c configs/all-singleton.json -db index.db -s org.byteflow.examples.NpeExamples -cp byteflow-core/build/classes/java/examples
```

### Gradle plugin

You can run the ByteFlow analyzer via Gradle plugin.

- First, assemble and publish everything locally:

```shell
./gradlew :core:publishToMavenLocal :gradle:publish
```

- Next, run the analyzer from the example project:

```shell
cd examples/byteflow-plugin-usage
./gradlew runAnalyzer
```
