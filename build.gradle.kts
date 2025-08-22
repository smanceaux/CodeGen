plugins {
    kotlin("jvm") version "2.0.10"
}

group = "cap.manchou.codegen"
version = "1.0-SNAPSHOT"

val commonsLang3Version = "3.18.0"
val hamcrestVersion = "3.0"
val jacksonVersion = "2.15.2"
val slf4jVersion = "2.20.0"

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
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$slf4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}
