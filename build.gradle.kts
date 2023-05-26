plugins {
    kotlin("jvm") version "1.8.20"
    java
    application
}

group = "org.goshik"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("us.codecraft:webmagic-core:0.8.0")
    implementation("us.codecraft:webmagic-extension:0.8.0")
    implementation("us.codecraft:webmagic-selenium:0.8.0")

    implementation("org.seleniumhq.selenium:selenium-java:3.141.59")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:3.141.59")


    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.slf4j:slf4j-log4j12:2.0.0")
    implementation("com.jcabi:jcabi-log:0.18.1")

    implementation("org.json:json:20210307")
    implementation("com.opencsv:opencsv:5.5.2")
    implementation("com.google.code.gson:gson:2.8.7")
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}