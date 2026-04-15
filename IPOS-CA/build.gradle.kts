plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.openjfx:javafx:25.0.2")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.github.librepdf:openpdf:1.3.30")
}

tasks.test {
    useJUnitPlatform()
}

javafx {
    version = "25.0.2"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("iposca.SystemLogin")
}