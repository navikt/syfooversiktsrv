package no.nav.syfo.application

data class ApplicationEnvironmentDatabase(
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
