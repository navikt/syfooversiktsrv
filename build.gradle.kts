import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0-SNAPSHOT"

val coroutinesVersion = "1.3.9"
val kotlinSerializationVersion = "0.9.0"
val ktorVersion = "1.3.2"
val flywayVersion = "5.2.4"
val jacksonVersion = "2.9.8"
val hikariVersion = "3.3.0"
val fuelVersion = "1.15.1"
val kafkaVersion = "2.0.0"
val kafkaEmbeddedVersion = "2.0.2"
val kluentVersion = "1.61"
val logbackVersion = "1.2.3"
val logstashEncoderVersion = "6.3"
val mockkVersion = "1.10.0"
val postgresTestContainersVersion = "1.14.3"
val postgresVersion = "42.2.13"
val prometheusVersion = "0.9.0"
val spekVersion = "2.0.12"
val smCommonVersion = "1.0.22"
val vaultJavaDriveVersion = "3.1.0"

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "no.nav.syfo.SyfooversiktApplicationKt"
}

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")

    // Database
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.bettercloud:vault-java-driver:$vaultJavaDriveVersion")

    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")
    implementation("no.nav.syfo.sm:syfosm-common-models:$smCommonVersion")
    implementation("no.nav.syfo.sm:syfosm-common-kafka:$smCommonVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlinSerializationVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")

    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:postgresql:$postgresTestContainersVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runtime-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")

    api("io.ktor:ktor-client-mock:$ktorVersion")
    api("io.ktor:ktor-client-mock-jvm:$ktorVersion")
}

tasks {
    create("printVersion") {
        println(project.version)
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}
