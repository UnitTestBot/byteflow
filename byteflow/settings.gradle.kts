rootProject.name = "byteflow"

fun myInclude(name: String) {
    include(name)
    project(":$name").projectDir = file("${rootProject.name}-$name")
}

myInclude("core")
myInclude("cli")
myInclude("gradle")
