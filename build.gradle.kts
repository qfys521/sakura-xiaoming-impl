plugins {
    kotlin("jvm") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "cn.qfys521"
version = "v0.0.1"

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
    compileOnly("cn.chuanwise.xiaoming:xiaomingbot:20250101-210305:all")

    // alibaba dashscope sdk
    implementation("com.alibaba:dashscope-sdk-java:2.21.1")

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