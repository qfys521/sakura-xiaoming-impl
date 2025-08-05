plugins {
    kotlin("jvm") version "2.2.0"
}

group = "cn.qfys521"
version = "1.0-SNAPSHOT"

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
    compileOnly("cn.chuanwise.xiaoming:xiaomingbot:20250101-210305")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}