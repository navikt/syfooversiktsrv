package no.nav.syfo.aktivitetskravvurdering

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.toPersonOversiktStatus
import no.nav.syfo.personstatus.db.*
import java.sql.Connection

fun persistAktivitetskrav(
    connection: Connection,
    aktivitetskrav: Aktivitetskrav,
) {
    val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
        fnr = aktivitetskrav.personIdent.value,
    ).firstOrNull()

    if (existingPersonOversiktStatus == null) {
        val personOversiktStatus = aktivitetskrav.toPersonOversiktStatus()
        connection.createPersonOversiktStatus(
            commit = false,
            personOversiktStatus = personOversiktStatus,
        )
    } else {
        connection.updatePersonOversiktStatusAktivitetskrav(
            pPersonOversiktStatus = existingPersonOversiktStatus,
            aktivitetskrav = aktivitetskrav,
        )
    }
}
