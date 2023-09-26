includeBuild("byteflow")
includeBuild("examples/byteflow-plugin-usage")
includeBuild("examples/byteflow-juliet")

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
