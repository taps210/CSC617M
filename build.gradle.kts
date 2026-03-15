plugins {
    java
    application
}

group = "csc617m"
version = "1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
    test {
        java.setSrcDirs(listOf("test"))
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("src.Main")
}

tasks.register<JavaExec>("runIDE") {
    group = "application"
    description = "Run Herd IDE"
    mainClass.set("src.gui.HerdIDE")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs("--add-opens", "java.desktop/java.awt=ALL-UNNAMED")
}

dependencies {
    implementation("com.formdev:flatlaf:3.4.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
