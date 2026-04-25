plugins {
    `java-library`
    application
}

group = "com.soby.sequencer"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.disruptor)
    implementation(libs.affinity)
    implementation(libs.hdrhistogram)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.soby.sequencer.Main")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Xlint:-processing")
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.jar {
    manifest {
        attributes(mapOf("Main-Class" to "com.soby.sequencer.Main"))
    }
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
