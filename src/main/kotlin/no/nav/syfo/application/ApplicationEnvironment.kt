package no.nav.syfo.application

import no.nav.syfo.application.cache.RedisEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment
import java.time.LocalDate

const val NAIS_DATABASE_ENV_PREFIX = "NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB"

data class Environment(
    val applicationName: String = "syfooversiktsrv",

    val azure: AzureEnvironment = AzureEnvironment(
        appClientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        appClientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
        appWellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
        openidConfigTokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    ),

    val database: DatabaseEnvironment = DatabaseEnvironment(
        host = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_HOST"),
        name = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_DATABASE"),
        port = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PORT"),
        password = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PASSWORD"),
        username = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_USERNAME"),
    ),

    val electorPath: String = getEnvVar("ELECTOR_PATH"),
    val arenaCutoff: LocalDate = LocalDate.parse(getEnvVar("ARENA_CUTOFF")),

    val kafka: KafkaEnvironment = KafkaEnvironment(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
        aivenSchemaRegistryUrl = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
        aivenRegistryUser = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
        aivenRegistryPassword = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
    ),

    val clients: ClientsEnvironment = ClientsEnvironment(
        ereg = ClientEnvironment(
            baseUrl = getEnvVar("EREG_URL"),
            clientId = "",
        ),
        pdl = ClientEnvironment(
            baseUrl = getEnvVar("PDL_URL"),
            clientId = getEnvVar("PDL_CLIENT_ID"),
        ),
        syfobehandlendeenhet = ClientEnvironment(
            baseUrl = getEnvVar("SYFOBEHANDLENDEENHET_URL"),
            clientId = getEnvVar("SYFOBEHANDLENDEENHET_CLIENT_ID"),
        ),
        istilgangskontroll = ClientEnvironment(
            baseUrl = getEnvVar("ISTILGANGSKONTROLL_HOST"),
            clientId = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
        ),
    ),

    val redis: RedisEnvironment = RedisEnvironment(
        host = getEnvVar("REDIS_HOST"),
        port = getEnvVar("REDIS_PORT", "6379").toInt(),
        secret = getEnvVar("REDIS_PASSWORD"),
    ),

    val cronjobBehandlendeEnhetIntervalDelayMinutes: Long = getEnvVar("CRONJOB_BEHANDLENDE_ENHET_INTERVAL_DELAY_MINUTES").toLong(),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
