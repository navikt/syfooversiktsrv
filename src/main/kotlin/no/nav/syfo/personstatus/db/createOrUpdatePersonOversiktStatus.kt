package no.nav.syfo.personstatus.db

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.personstatus.createPersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.updatePersonOppfolgingstilfelleVirksomhetList
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
        name,
        tildelt_veileder,
        tildelt_enhet,
        tildelt_enhet_updated_at,
        opprettet,
        sist_endret,
        motebehov_ubehandlet,
        moteplanlegger_ubehandlet,
        oppfolgingsplan_lps_bistand_ubehandlet,
        dialogmotesvar_ubehandlet,
        oppfolgingstilfelle_updated_at,
        oppfolgingstilfelle_generated_at,
        oppfolgingstilfelle_start,
        oppfolgingstilfelle_end,
        oppfolgingstilfelle_bit_referanse_uuid,
        oppfolgingstilfelle_bit_referanse_inntruffet,
        dialogmotekandidat,
        dialogmotekandidat_generated_at,
        motestatus,
        motestatus_generated_at,
        aktivitetskrav,
        aktivitetskrav_stoppunkt,
        aktivitetskrav_updated_at
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        if (personOversiktStatus.enhet != null) {
            it.setObject(6, now)
        } else {
            it.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE)
        }
        it.setTimestamp(7, tidspunkt)
        it.setTimestamp(8, tidspunkt)
        if (personOversiktStatus.motebehovUbehandlet != null) {
            it.setBoolean(9, personOversiktStatus.motebehovUbehandlet)
        } else {
            it.setNull(9, NULL)
        }
        it.setNull(10, NULL)
        if (personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet != null) {
            it.setBoolean(11, personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
        } else {
            it.setNull(11, NULL)
        }
        it.setBoolean(12, personOversiktStatus.dialogmotesvarUbehandlet)
        it.setObject(13, personOversiktStatus.latestOppfolgingstilfelle?.updatedAt)
        it.setObject(14, personOversiktStatus.latestOppfolgingstilfelle?.generatedAt)
        it.setObject(15, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleStart)
        it.setObject(16, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleEnd)
        if (personOversiktStatus.latestOppfolgingstilfelle != null) {
            it.setString(
                17,
                personOversiktStatus.latestOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid.toString()
            )
        } else {
            it.setNull(17, Types.CHAR)
        }
        it.setObject(18, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseInntruffet)
        it.setObject(19, personOversiktStatus.dialogmotekandidat)
        it.setObject(20, personOversiktStatus.dialogmotekandidatGeneratedAt)
        it.setString(21, personOversiktStatus.motestatus)
        it.setObject(22, personOversiktStatus.motestatusGeneratedAt)
        it.setString(23, personOversiktStatus.aktivitetskrav?.name)
        it.setObject(24, personOversiktStatus.aktivitetskravStoppunkt)
        it.setObject(25, personOversiktStatus.aktivitetskravUpdatedAt)
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
    oppfolgingsplan_lps_bistand_ubehandlet = ?,
    dialogmotesvar_ubehandlet = ?
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
            it.setNull(5, NULL)
            if (personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet != null) {
                it.setBoolean(6, personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            } else {
                it.setNull(6, NULL)
            }
            it.setBoolean(7, personOversiktStatus.dialogmotesvarUbehandlet)
            it.setString(8, personOversiktStatus.fnr)
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
    oppfolgingstilfelle: Oppfolgingstilfelle,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusOppfolgingstilfelle).use {
        it.setObject(1, oppfolgingstilfelle.updatedAt)
        it.setObject(2, oppfolgingstilfelle.generatedAt)
        it.setObject(3, oppfolgingstilfelle.oppfolgingstilfelleStart)
        it.setObject(4, oppfolgingstilfelle.oppfolgingstilfelleEnd)
        it.setString(5, oppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid.toString())
        it.setObject(6, oppfolgingstilfelle.oppfolgingstilfelleBitReferanseInntruffet)
        it.setString(7, pPersonOversiktStatus.fnr)
        it.execute()
    }
    updatePersonOppfolgingstilfelleVirksomhetList(
        personOversiktStatusId = pPersonOversiktStatus.id,
        personOppfolgingstilfelleVirksomhetList = oppfolgingstilfelle.virksomhetList,
    )
}

fun DatabaseInterface.lagreBrukerKnytningPaEnhet(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val id = oppdaterEnhetDersomKnytningFinnes(veilederBrukerKnytning)

    if (id == KNYTNING_IKKE_FUNNET) {
        val personOversiktStatus = PersonOversiktStatus(
            veilederIdent = veilederBrukerKnytning.veilederIdent,
            fnr = veilederBrukerKnytning.fnr,
            navn = null,
            enhet = null,
            motebehovUbehandlet = null,
            oppfolgingsplanLPSBistandUbehandlet = null,
            dialogmotesvarUbehandlet = false,
            dialogmotekandidat = null,
            dialogmotekandidatGeneratedAt = null,
            motestatus = null,
            motestatusGeneratedAt = null,
            latestOppfolgingstilfelle = null,
            aktivitetskrav = null,
            aktivitetskravStoppunkt = null,
            aktivitetskravUpdatedAt = null,
        )
        this.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personOversiktStatus,
            )
        }
    }
}

const val queryUpdatePersonOversiktStatusNavn =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET name = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusNavn(
    personIdentNameMap: Map<String, String>,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonOversiktStatusNavn).use {
            personIdentNameMap.forEach { (personident, navn) ->
                it.setString(1, navn)
                it.setString(2, personident)
                it.executeUpdate()
            }
        }
    }
    connection.commit()
}
