package no.nav.syfo.application

data class Environment(
    val applicationName: String = "syfooversiktsrv",

    val azureAppClientId: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val azureTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),

    val databaseHost: String = getEnvVar("NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB_HOST"),
    val databasePort: String = getEnvVar("NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB_PORT"),
    val databaseName: String = getEnvVar("NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB_DATABASE"),
    val databaseUsername: String = getEnvVar("NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB_USERNAME"),
    val databasePassword: String = getEnvVar("NAIS_DATABASE_SYFOOVERSIKTSRV_SYFOOVERSIKTSRV_DB_PASSWORD"),

    val kafkaBootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val kafkaSchemaRegistryUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),

    val oversikthendelseOppfolgingstilfelleTopic: String = "aapen-syfo-oversikthendelse-tilfelle-v1",

    val redisHost: String = getEnvVar("REDIS_HOST"),
    val redisPort: Int = getEnvVar("REDIS_PORT", "6379").toInt(),
    val redisSecret: String = getEnvVar("REDIS_PASSWORD"),

    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),

    val syfotilgangskontrollClientId: String = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
    val syfotilgangskontrollUrl: String = getEnvVar("SYFOTILGANGSKONTROLL_URL"),

    val toggleKafkaConsumerEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_CONSUMER_ENABLED").toBoolean()
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$databaseHost:$databasePort/$databaseName"
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
