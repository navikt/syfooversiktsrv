package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.util.nowUTC
import java.sql.Connection
import java.sql.Timestamp
import java.sql.Types.NULL
import java.time.Instant
import java.util.*

const val queryCreatePersonOversiktStatus =
    """
    INSERT INTO PERSON_OVERSIKT_STATUS (
        id,
        uuid,
        fnr,
        navn,
        tildelt_veileder,
        tildelt_enhet,
        tildelt_enhet_updated_at,
        opprettet,
        sist_endret,
        motebehov_ubehandlet,
        moteplanlegger_ubehandlet,
        oppfolgingsplan_lps_bistand_ubehandlet
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """

fun Connection.createPersonOversiktStatus(
    commit: Boolean,
    personOversiktStatus: PersonOversiktStatus,
) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())
    val now = nowUTC()

    this.prepareStatement(queryCreatePersonOversiktStatus).use {
        it.setString(1, uuid)
        it.setString(2, personOversiktStatus.fnr)
        it.setString(3, personOversiktStatus.navn)
        it.setString(4, personOversiktStatus.veilederIdent)
        it.setString(5, personOversiktStatus.enhet)
        it.setObject(6, now)
        it.setTimestamp(7, tidspunkt)
        it.setTimestamp(8, tidspunkt)
        if (personOversiktStatus.motebehovUbehandlet != null) {
            it.setBoolean(9, personOversiktStatus.motebehovUbehandlet)
        } else {
            it.setNull(9, NULL)
        }
        if (personOversiktStatus.moteplanleggerUbehandlet != null) {
            it.setBoolean(10, personOversiktStatus.moteplanleggerUbehandlet)
        } else {
            it.setNull(10, NULL)
        }
        if (personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet != null) {
            it.setBoolean(11, personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
        } else {
            it.setNull(11, NULL)
        }
        it.execute()
    }
    if (commit) {
        this.commit()
    }
}

const val queryUpdatePersonOversiktStatus =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_veileder = ?,
    tildelt_enhet = ?,
    sist_endret = ?,
    motebehov_ubehandlet = ?,
    moteplanlegger_ubehandlet = ?,
    oppfolgingsplan_lps_bistand_ubehandlet = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatus(
    personOversiktStatus: PersonOversiktStatus,
) {
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonOversiktStatus).use {
            it.setString(1, personOversiktStatus.veilederIdent)
            it.setString(2, personOversiktStatus.enhet)
            it.setTimestamp(3, tidspunkt)
            if (personOversiktStatus.motebehovUbehandlet != null) {
                it.setBoolean(4, personOversiktStatus.motebehovUbehandlet)
            } else {
                it.setNull(4, NULL)
            }
            if (personOversiktStatus.moteplanleggerUbehandlet != null) {
                it.setBoolean(5, personOversiktStatus.moteplanleggerUbehandlet)
            } else {
                it.setNull(5, NULL)
            }
            if (personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet != null) {
                it.setBoolean(6, personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            } else {
                it.setNull(6, NULL)
            }
            it.setString(7, personOversiktStatus.fnr)
            it.execute()
        }
        connection.commit()
    }
}
