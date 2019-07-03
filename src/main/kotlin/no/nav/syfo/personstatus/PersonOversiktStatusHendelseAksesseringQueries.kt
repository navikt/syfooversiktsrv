package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.personstatus.domain.KOversikthendelse
import java.sql.Timestamp
import java.sql.Types.NULL
import java.time.Instant
import java.util.*

fun DatabaseInterface.oppdaterPersonMedMotebehovMottattNyEnhet(oversiktHendelse: KOversikthendelse) {
    val tidspunkt = Timestamp.from(Instant.now())
    val query = """
                        UPDATE PERSON_OVERSIKT_STATUS
                        SET tildelt_veileder = ?, motebehov_ubehandlet = ?, tildelt_enhet = ?, sist_endret = ?
                        WHERE fnr = ?
                """
    connection.use { connection ->
        connection.prepareStatement(query).use {
            it.setNull(1, NULL)
            it.setBoolean(2, true)
            it.setString(3, oversiktHendelse.enhetId)
            it.setTimestamp(4, tidspunkt)
            it.setString(5, oversiktHendelse.fnr)
            it.execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.oppdaterPersonMedMotebehovMottatt(oversiktHendelse: KOversikthendelse) {
    val tidspunkt = Timestamp.from(Instant.now())

    val query = """
                        UPDATE PERSON_OVERSIKT_STATUS
                        SET motebehov_ubehandlet = ?, tildelt_enhet = ?, sist_endret = ?
                        WHERE fnr = ?
                """
    connection.use { connection ->
        connection.prepareStatement(query).use {
            it.setBoolean(1, true)
            it.setString(2, oversiktHendelse.enhetId)
            it.setTimestamp(3, tidspunkt)
            it.setString(4, oversiktHendelse.fnr)
            it.execute()
        }
        connection.commit()
    }
}

const val queryOpprettPersonMedMotebehovMottatt = """INSERT INTO PERSON_OVERSIKT_STATUS (
        id,
        uuid,
        fnr,
        tildelt_enhet,
        motebehov_ubehandlet,
        opprettet,
        sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)"""

fun DatabaseInterface.opprettPersonMedMotebehovMottatt(oversiktHendelse: KOversikthendelse) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOpprettPersonMedMotebehovMottatt).use {
            it.setString(1, uuid)
            it.setString(2, oversiktHendelse.fnr)
            it.setString(3, oversiktHendelse.enhetId)
            it.setBoolean(4, true)
            it.setTimestamp(5, tidspunkt)
            it.setTimestamp(6, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}