import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0-SNAPSHOT"

val confluent = "7.9.0"
val flyway = "11.14.0"
val hikari = "7.0.2"
val isdialogmoteSchema = "1.0.5"
val jacksonDataType = "2.20.0"
val jedis = "5.2.0"
val json = "20240303"
val kafka = "3.9.0"
val ktor = "3.3.1"
val logback = "1.5.19"
val logstashEncoder = "7.4"
val mockk = "1.14.6"
val micrometerRegistry = "1.15.4"
val nimbusjosejwt = "10.5"
val postgresEmbedded = "2.1.1"
val postgres = "42.7.8"
val postgresRuntimeVersion = "17.6.0"

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.gradleup.shadow") version "8.3.6"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.8.0"
    id("com.adarshr.test-logger") version "4.0.0"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.ktor:ktor-server-auth-jwt:$ktor")
    implementation("io.ktor:ktor-client-apache:$ktor")
    implementation("io.ktor:ktor-client-cio:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-jackson:$ktor")
    implementation("io.ktor:ktor-server-call-id:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-status-pages:$ktor")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoder")
    implementation("org.json:json:$json")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktor")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistry")

    // Cache
    implementation("redis.clients:jedis:$jedis")

    // (De-)serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonDataType")

    // Database
    implementation("org.postgresql:postgresql:$postgres")
    implementation("com.zaxxer:HikariCP:$hikari")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway")
    testImplementation("io.zonky.test:embedded-postgres:$postgresEmbedded")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:$postgresRuntimeVersion"))

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:$kafka", excludeLog4j)
    implementation("io.confluent:kafka-avro-serializer:$confluent", excludeLog4j)
    constraints {
        implementation("org.apache.zookeeper:zookeeper") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-44981")
            version {
                require("3.9.3")
            }
        }
        implementation("org.apache.avro:avro") {
            because("io.confluent:kafka-schema-registry:$confluent -> https://www.cve.org/CVERecord?id=CVE-2023-39410")
            version {
                require("1.12.0")
            }
        }
        implementation("org.apache.commons:commons-compress") {
            because("org.apache.commons:commons-compress:1.22 -> https://www.cve.org/CVERecord?id=CVE-2012-2098")
            version {
                require("1.27.1")
            }
        }
    }
    implementation("no.nav.syfo.dialogmote.avro:isdialogmote-schema:$isdialogmoteSchema")

    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusjosejwt")
    testImplementation("io.ktor:ktor-server-test-host:$ktor")
    testImplementation("io.ktor:ktor-client-mock:$ktor")
    testImplementation("io.mockk:mockk:$mockk")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        println(project.version)
    }

    shadowJar {
        mergeServiceFiles()
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    withType<KotlinCompile> {
        dependsOn(":generateAvroJava")
        dependsOn(":generateTestAvroJava")
    }

    test {
        useJUnitPlatform()
        testlogger {
            theme = ThemeType.STANDARD_PARALLEL
            showFullStackTraces = true
            showPassed = false
        }
    }
}
