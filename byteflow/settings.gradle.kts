rootProject.name = "byteflow"

fun myInclude(name: String) {
    include(name)
    project(":$name").projectDir = file("${rootProject.name}-$name")
}

myInclude("core")
myInclude("cli")
myInclude("gradle")

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
