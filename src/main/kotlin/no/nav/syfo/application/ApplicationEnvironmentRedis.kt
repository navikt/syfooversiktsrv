package no.nav.syfo.application

data class ApplicationEnvironmentRedis(
    val host: String,
    val port: Int,
    val secret: String,
)
