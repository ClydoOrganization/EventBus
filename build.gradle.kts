plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    withSourcesJar()
    withJavadocJar()
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    listOf(
        "org.projectlombok:lombok:1.18.42",
        "org.jetbrains:annotations:26.1.0"
    ).forEach {
        compileOnly(it)
        annotationProcessor(it)
    }

    api("com.github.ClydoOrganization:Clytil:a7cf7ff26c")
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

tasks.wrapper {
    gradleVersion = "9.7.1"
    distributionType = Wrapper.DistributionType.BIN
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
