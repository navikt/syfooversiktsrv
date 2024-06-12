package no.nav.syfo.personstatus.infrastructure.cronjob

interface Cronjob {
    suspend fun run()
    val initialDelayMinutes: Long
    val intervalDelayMinutes: Long
}
