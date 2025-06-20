plugins {
    kotlin("jvm") version "1.9.23"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.44.0")
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

application {
    mainClass.set("LsrKt") // имя класса с main функцией (файл Lsr.kt)
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
}
