package no.nav.syfo.application

data class Environment(
    val applicationName: String = "syfooversiktsrv",

    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val azureTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),

    val syfotilgangskontrollClientId: String = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
    val syfotilgangskontrollUrl: String = getEnvVar("SYFOTILGANGSKONTROLL_URL"),

    val databaseName: String = getEnvVar("DATABASE_NAME", "syfooversiktsrv"),
    val syfooversiktsrvDBURL: String = getEnvVar("SYFOOVERSIKTSRV_DB_URL"),

    val mountPathVault: String = getEnvVar("MOUNT_PATH_VAULT"),
    val oversikthendelseOppfolgingstilfelleTopic: String = "aapen-syfo-oversikthendelse-tilfelle-v1",

    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val kafkaSchemaRegistryUrl: String = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
)

data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String,
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
