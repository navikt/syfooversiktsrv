package no.nav.syfo.personstatus.infrastructure.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository

class PersonOversiktStatusRepository(private val database: DatabaseInterface) : IPersonOversiktStatusRepository {

    override fun updateArbeidsuforhetVurderingStatus(personIdent: PersonIdent, isAktivVurdering: Boolean): Int {
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_ARBEIDSUFORHET_VURDERING_STATUS).use {
                it.setBoolean(1, isAktivVurdering)
                it.setString(2, personIdent.value)
                val rowsUpdated = it.executeUpdate()
                connection.commit()
                return rowsUpdated
            }
        }
    }

    companion object {
        private const val UPDATE_ARBEIDSUFORHET_VURDERING_STATUS =
            """
            UPDATE person_oversikt_status
            SET arbeidsuforhet_aktiv_vurdering = ?
            WHERE fnr = ?
            """
    }
}
