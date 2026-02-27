plugins {
    id("java")
    id("maven-publish")
}

group = "net.clydo.eventbus"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

repositories {
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

    implementation("com.google.guava:guava:33.5.0-jre")
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

tasks.wrapper {
    gradleVersion = "9.0.0"
    distributionType = Wrapper.DistributionType.ALL
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
