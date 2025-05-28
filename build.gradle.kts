plugins {
    id("java")
    id("maven-publish")
}

group = "net.clydo.eventbus"
version = "1.0.0"

val javaVersion = 17

java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
java.sourceCompatibility = JavaVersion.toVersion(javaVersion)
java.targetCompatibility = JavaVersion.toVersion(javaVersion)

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    compileOnly("org.jetbrains:annotations:26.0.2")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.google.guava:guava:33.4.8-jre")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "8.13"
    distributionType = Wrapper.DistributionType.ALL
}
