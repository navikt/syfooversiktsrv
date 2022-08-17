package no.nav.syfo.application

import no.nav.syfo.application.cache.RedisEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment

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

    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val kafkaSchemaRegistryUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),

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
    val kafkaOppfolgingstilfellePersonProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_OPPFOLGINGSTILFELLE_PERSON_PROCESSING_ENABLED").toBoolean(),
    val kafkaDialogmotekandidatProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_DIALOGMOTEKANDIDAT_PROCESSING_ENABLED").toBoolean(),
    val kafkaDialogmoteStatusendringProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_DIALOGMOTE_STATUSENDRING_PROCESSING_ENABLED").toBoolean(),
    val kafkaPersonoppgavehendelseProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_PERSONOPPGAVEHENDELSE_PROCESSING_ENABLED").toBoolean(),

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
        syfotilgangskontroll = ClientEnvironment(
            baseUrl = getEnvVar("SYFOTILGANGSKONTROLL_URL"),
            clientId = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
        ),
    ),

    val redis: RedisEnvironment = RedisEnvironment(
        host = getEnvVar("REDIS_HOST"),
        port = getEnvVar("REDIS_PORT", "6379").toInt(),
        secret = getEnvVar("REDIS_PASSWORD"),
    ),

    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),

    val personBehandlendeEnhetCronjobEnabled: Boolean = getEnvVar("TOGGLE_PERSON_BEHANDLENDE_ENHET_CRONJOB_ENABLED").toBoolean(),
    val personOppfolgingstilfelleVirksomhetsnavnCronjobEnabled: Boolean = getEnvVar("TOGGLE_PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_CRONJOB_ENABLED").toBoolean(),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
