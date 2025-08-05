plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-rc3"
    id("maven-publish")

}

group = "cn.qfys521"
version = "v1.1.0"

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

    // alibaba dashscope sdk
    implementation("com.alibaba:dashscope-sdk-java:2.21.1")

    // fastjson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.+")

    testImplementation(kotlin("test"))
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