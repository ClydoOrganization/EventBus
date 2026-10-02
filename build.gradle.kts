plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    id("io.freefair.lombok") version "9.7.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    withSourcesJar()
    withJavadocJar()
}

lombok {
    version = "1.18.42"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.1.0")

    api("com.github.ClydoOrganization:Clytil:a7cf7ff26c")

    implementation("org.slf4j:slf4j-api:2.0.19")
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
