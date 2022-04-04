package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.nowUTC
import java.sql.*
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
        oppfolgingsplan_lps_bistand_ubehandlet,
        oppfolgingstilfelle_updated_at,
        oppfolgingstilfelle_generated_at,
        oppfolgingstilfelle_start,
        oppfolgingstilfelle_end,
        oppfolgingstilfelle_bit_referanse_uuid,
        oppfolgingstilfelle_bit_referanse_inntruffet
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    RETURNING id
    """

fun Connection.createPersonOversiktStatus(
    commit: Boolean,
    personOversiktStatus: PersonOversiktStatus,
) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())
    val now = nowUTC()

    val personOversiktStatusId: Int = this.prepareStatement(queryCreatePersonOversiktStatus).use {
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
        it.setObject(12, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleUpdatedAt)
        it.setObject(13, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleGeneratedAt)
        it.setObject(14, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleStart)
        it.setObject(15, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleEnd)
        if (personOversiktStatus.latestOppfolgingstilfelle != null) {
            it.setString(
                16,
                personOversiktStatus.latestOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid.toString()
            )
        } else {
            it.setNull(16, Types.CHAR)
        }
        it.setObject(17, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseInntruffet)
        it.executeQuery().toList { getInt("id") }.firstOrNull()
    } ?: throw SQLException("Creating PersonOversikStatus failed, no rows affected.")

    personOversiktStatus.latestOppfolgingstilfelle?.let { personOppfolgingstilfelle ->
        createPersonOppfolgingstilfelleVirksomhetList(
            commit = commit,
            personOversiktStatusId = personOversiktStatusId,
            personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelle.virksomhetList,
        )
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

const val queryUpdatePersonOversiktStatusOppfolgingstilfelle =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET oppfolgingstilfelle_updated_at = ?,
    oppfolgingstilfelle_generated_at = ?,
    oppfolgingstilfelle_start = ?,
    oppfolgingstilfelle_end = ?,
    oppfolgingstilfelle_bit_referanse_uuid = ?,
    oppfolgingstilfelle_bit_referanse_inntruffet = ?
    WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusOppfolgingstilfelle(
    pPersonOversiktStatus: PPersonOversiktStatus,
    personOppfolgingstilfelle: PersonOppfolgingstilfelle,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusOppfolgingstilfelle).use {
        it.setObject(1, personOppfolgingstilfelle.oppfolgingstilfelleUpdatedAt)
        it.setObject(2, personOppfolgingstilfelle.oppfolgingstilfelleGeneratedAt)
        it.setObject(3, personOppfolgingstilfelle.oppfolgingstilfelleStart)
        it.setObject(4, personOppfolgingstilfelle.oppfolgingstilfelleEnd)
        it.setString(5, personOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid.toString())
        it.setObject(6, personOppfolgingstilfelle.oppfolgingstilfelleBitReferanseInntruffet)
        it.setString(7, pPersonOversiktStatus.fnr)
        it.execute()
    }
    updatePersonOppfolgingstilfelleVirksomhetList(
        personOversiktStatusId = pPersonOversiktStatus.id,
        personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelle.virksomhetList,
    )
}
