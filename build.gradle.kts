plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runRecording") {
    group = "application"
    description = "Run the Recording Debugger"
    mainClass.set("dbg.RecordingDebugger")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runDebugger") {
    group = "application"
    description = "Run the standard Debugger"
    mainClass.set("dbg.JDISimpleDebugger")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runGUI") {
    group = "application"
    description = "Run the Debugger with GUI"
    mainClass.set("gui.JDISimpleDebuggerGUI")
    classpath = sourceSets["main"].runtimeClasspath
}

