package no.nav.syfo.personstatus.infrastructure.cronjob

data class CronjobResult(
    var updated: Int = 0,
    var failed: Int = 0,
)
