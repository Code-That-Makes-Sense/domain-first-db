plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("me.champeau.jmh") version "0.7.3"
}

group = "com.codethatmakessense"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    jmhImplementation(sourceSets.test.get().output)
    jmhImplementation(sourceSets.test.get().runtimeClasspath)
    jmhImplementation("org.junit.platform:junit-platform-launcher")
}

jmh {
    jmhVersion = "1.37"
    resultFormat = "JSON"
    resultsFile = layout.buildDirectory.file("reports/jmh/results.json")
    jvmArgs = listOf("-Djmh.ignoreLock=true")
}

val benchmarkReport by tasks.registering(Exec::class) {
    description = "Turns the JMH results into docs/benchmark.md."
    group = "verification"
    commandLine("python3", "scripts/benchmark-report.py",
        layout.buildDirectory.file("reports/jmh/results.json").get().asFile.path, "docs/benchmark.md")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val fastTest by tasks.registering(Test::class) {
    description = "Runs every test that does not boot Spring or touch a database."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        excludeTags("boots-spring")
    }
}
