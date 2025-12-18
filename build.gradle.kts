plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-rc3"
    id("maven-publish")

}

group = "cn.qfys521"
version = "v1.3.1"

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

    // 使用 Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.+")

    // OkHttp 用于 HTTP 调用
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

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
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveBaseName.set("sakura-xiaoming-impl")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    minimize()
    manifest {
        attributes(
            "Implementation-Title" to "Sakura XiaoMing Implementation",
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to project.group.toString(),
            "Xiaoming-Version" to "4.9.10-20250101-210305",
            "Java-Version" to "21",
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