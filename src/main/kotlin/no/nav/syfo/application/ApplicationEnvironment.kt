package no.nav.syfo.application

const val NAIS_DATABASE_ENV_PREFIX = "NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB"

data class Environment(
    val applicationName: String = "syfooversiktsrv",

    val azure: ApplicationEnvironmentAzure = ApplicationEnvironmentAzure(
        appClientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        appClientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
        appWellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
        tokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    ),

    val database: ApplicationEnvironmentDatabase = ApplicationEnvironmentDatabase(
        host = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_HOST"),
        name = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_DATABASE"),
        port = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PORT"),
        password = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PASSWORD"),
        username = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_USERNAME"),
    ),

    val electorPath: String = getEnvVar("ELECTOR_PATH"),

    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val kafkaSchemaRegistryUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),

    val kafka: ApplicationEnvironmentKafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    ),
    val kafkaOversikthendelsetilfelleProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_OVERSIKTHENDELSETILFELLE_ENABLED").toBoolean(),
    val kafkaOppfolgingstilfellePersonProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_OPPFOLGINGSTILFELLE_PERSON_PROCESSING_ENABLED").toBoolean(),

    val oversikthendelseOppfolgingstilfelleTopic: String = "aapen-syfo-oversikthendelse-tilfelle-v1",

    val isproxyClientId: String = getEnvVar("ISPROXY_CLIENT_ID"),
    val isproxyUrl: String = getEnvVar("ISPROXY_URL"),

    val pdlClientId: String = getEnvVar("PDL_CLIENT_ID"),
    val pdlUrl: String = getEnvVar("PDL_URL"),

    val redisHost: String = getEnvVar("REDIS_HOST"),
    val redisPort: Int = getEnvVar("REDIS_PORT", "6379").toInt(),
    val redisSecret: String = getEnvVar("REDIS_PASSWORD"),

    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),

    val syfobehandlendeenhetClientId: String = getEnvVar("SYFOBEHANDLENDEENHET_CLIENT_ID"),
    val syfobehandlendeenhetUrl: String = getEnvVar("SYFOBEHANDLENDEENHET_URL"),

    val syfotilgangskontrollClientId: String = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
    val syfotilgangskontrollUrl: String = getEnvVar("SYFOTILGANGSKONTROLL_URL"),

    val personBehandlendeEnhetCronjobEnabled: Boolean = getEnvVar("TOGGLE_PERSON_BEHANDLENDE_ENHET_CRONJOB_ENABLED").toBoolean(),
    val personOppfolgingstilfelleVirksomhetsnavnCronjobEnabled: Boolean = getEnvVar("TOGGLE_PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_CRONJOB_ENABLED").toBoolean(),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
