plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-rc3"
    id("maven-publish")

}

group = "cn.qfys521"
version = "v1.4.5-alpha7"

repositories {
    mavenCentral()
    maven {
        name = "Aliyun Public Maven"
        url = uri("https://maven.aliyun.com/repository/public")
    }
    mavenLocal()
}

dependencies {

    // xiao-ming bot
    compileOnly(files("libs/xiaomingbot-20250101-210305-all.jar"))
    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.+")
    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Codex Java SDK (0.0.7), backed by the local codex app-server
    implementation(files("libs/codex-java-sdk-0.0.7.jar"))

    testImplementation(kotlin("test"))
}

tasks.register("updateResourcesVersion") {
    val path = "src/main/resources/xiaoming.json"
    val file = file(path)
    if (file.exists()) {
        val lines = file.readLines().toMutableList()
        for (i in lines.indices) {
            if (lines[i].contains("\"version\":")) {
                lines[i] = "  \"version\": \"${project.version}\","
                break
            }
        }
        file.writeText(lines.joinToString("\n"))
        logger.info("Updated version in $path to ${project.version}")

    } else {
        logger.error("File $path does not exist.")
    }
}
tasks.named("processResources") {
    dependsOn("updateResourcesVersion")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    // The SDK is compiled for Java 25 and therefore requires a Java 25 runtime.
    // Kotlin 2.2 currently supports JVM bytecode targets through 24, so compile
    // the plugin bytecode for 24 while running it on the required JDK 25.
    jvmToolchain(25)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.release.set(24)
}

tasks.shadowJar {
    archiveBaseName.set("sakura-xiaoming-impl")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    // Keep SDK protocol classes that may be loaded reflectively by Jackson/app-server.
    manifest {
        attributes(
            "Implementation-Title" to "Sakura XiaoMing Implementation",
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to project.group.toString(),
            "Xiaoming-Version" to "4.9.10-20250101-210305",
            "Java-Version" to "25",
            "LIcense" to "AGPL-3.0",
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "sakura-xiaoming-impl"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
