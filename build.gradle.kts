plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.1"
}

group = "com.nick"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    compileOnly("dev.invisiblespiders.haven:haven-api:1.0.3")
    compileOnly("com.invisiblespiders:havenclaims-api:1.7.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("dev.invisiblespiders.haven:haven-api:1.0.3")
    testImplementation("com.invisiblespiders:havenclaims-api:1.7.0-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("HavenTeleport")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
