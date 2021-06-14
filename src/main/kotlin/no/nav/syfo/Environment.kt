package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.kafka.KafkaConfig
import no.nav.syfo.kafka.KafkaCredentials
import java.io.File

const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"
const val defaultlocalEnvironmentPropertiesPath = "./src/main/resources/localEnvForTests.json"
private val objectMapper: ObjectMapper = ObjectMapper()

fun getEnvironment(): Environment {
    objectMapper.registerKotlinModule()
    return if (appIsRunningLocally) {
        objectMapper.readValue(firstExistingFile(localEnvironmentPropertiesPath, defaultlocalEnvironmentPropertiesPath), Environment::class.java)
    } else {
        Environment(
            applicationPort = getEnvVar("APPLICATION_PORT", "8080").toInt(),
            applicationThreads = getEnvVar("APPLICATION_THREADS", "1").toInt(),
            applicationName = getEnvVar("APPLICATION_NAME", "syfooversiktsrv"),
            aadDiscoveryUrl = getEnvVar("AADDISCOVERY_URL"),
            azureAppClientId = getEnvVar("AZURE_APP_CLIENT_ID"),
            azureAppClientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
            azureAppWellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
            azureTokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            jwkKeysUrl = getEnvVar("JWKKEYS_URL", "https://login.microsoftonline.com/common/discovery/keys"),
            jwtIssuer = getEnvVar("JWT_ISSUER"),
            syfotilgangskontrollClientId = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
            syfotilgangskontrollUrl = getEnvVar("SYFOTILGANGSKONTROLL_URL"),
            databaseName = getEnvVar("DATABASE_NAME", "syfooversiktsrv"),
            syfooversiktsrvDBURL = getEnvVar("SYFOOVERSIKTSRV_DB_URL"),
            mountPathVault = getEnvVar("MOUNT_PATH_VAULT"),
            oversikthendelseOppfolgingstilfelleTopic = getEnvVar("OVERSIKTHENDELSE_OPPFOLGINGSTILFELLE_TOPIC", "aapen-syfo-oversikthendelse-tilfelle-v1"),
            kafkaBootstrapServers = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
            clientid = getEnvVar("CLIENT_ID")
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
    val applicationPort: Int,
    val applicationThreads: Int,
    val applicationName: String,
    val aadDiscoveryUrl: String,
    val azureAppClientId: String,
    val azureAppClientSecret: String,
    val azureAppWellKnownUrl: String,
    val azureTokenEndpoint: String,
    val jwkKeysUrl: String,
    val jwtIssuer: String,
    val syfotilgangskontrollClientId: String,
    val syfotilgangskontrollUrl: String,
    val databaseName: String,
    val syfooversiktsrvDBURL: String,
    val mountPathVault: String,
    val oversikthendelseOppfolgingstilfelleTopic: String,
    override val kafkaBootstrapServers: String,
    val clientid: String
) : KafkaConfig

data class VaultSecrets(
    val serviceuserUsername: String,
    val serviceuserPassword: String
) : KafkaCredentials {
    override val kafkaUsername: String = serviceuserUsername
    override val kafkaPassword: String = serviceuserPassword
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
    .map(::File)
    .first(File::exists)
