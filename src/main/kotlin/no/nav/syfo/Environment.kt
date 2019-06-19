package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/credentials.json"
const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"
const val defaultlocalEnvironmentPropertiesPath = "./src/main/resources/localEnvForTests.json"
private val objectMapper: ObjectMapper = ObjectMapper()

fun getEnvironment(): Environment {
    objectMapper.registerKotlinModule()
    return if (appIsRunningLocally) {
        objectMapper.readValue(firstExistingFile(localEnvironmentPropertiesPath, defaultlocalEnvironmentPropertiesPath), Environment::class.java)
    } else {
        Environment(
                getEnvVar("APPLICATION_PORT", "8080").toInt(),
                getEnvVar("APPLICATION_THREADS", "4").toInt(),
                getEnvVar("AADDISCOVERY_URL"),
                getEnvVar("JWKKEYS_URL", "https://login.microsoftonline.com/common/discovery/keys"),
                getEnvVar("JWT_ISSUER"),
                getEnvVar("SYFOTILGANGSKONTROLL_URL", "http://syfo-tilgangskontroll"),
                getEnvVar("DATABASE_NAME", "syfooversiktsrv"),
                getEnvVar("SYFOOVERSIKTSRV_DB_URL"),
                getEnvVar("MOUNT_PATH_VAULT"),
                getEnvVar("CLIENT_ID")
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
        val applicationPort: Int,
        val applicationThreads: Int,
        val aadDiscoveryUrl: String,
        val jwkKeysUrl: String,
        val jwtIssuer: String,
        val syfotilgangskontrollUrl: String,
        val databaseName: String,
        val syfooversiktsrvDBURL: String,
        val mountPathVault: String,
        val clientid: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
        .map(::File)
        .first(File::exists)
