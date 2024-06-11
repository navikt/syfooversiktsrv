package no.nav.syfo.personstatus.infrastructure.database

data class DatabaseEnvironment(
    val host: String,
    val name: String,
    val port: String,
    val password: String,
    val username: String,
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$host:$port/$name"
    }
}
