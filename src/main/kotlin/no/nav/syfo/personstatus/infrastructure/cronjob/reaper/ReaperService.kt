package no.nav.syfo.personstatus.infrastructure.cronjob.reaper

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import java.util.*

class ReaperService(
    private val database: DatabaseInterface,
) {
    fun getPersonerForReaper(): List<UUID> {
        return database.getPersonerWithOutdatedVeiledertildeling().map { it.uuid }
    }

    fun reap(uuid: UUID) {
        database.resetTildeltVeileder(uuid)
    }
}
