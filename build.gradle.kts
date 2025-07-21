plugins {
    kotlin("jvm") version "2.0.10"
}

group = "cap.manchou.codegen"
version = "1.0-SNAPSHOT"

val commonsLang3Version = "3.18.0"
val hamcrestVersion = "3.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
}
