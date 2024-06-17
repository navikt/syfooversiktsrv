package no.nav.syfo.personstatus.db

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.toList
import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
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
        aktivitetskrav_sist_vurdert,
        aktivitetskrav_vurdering_frist,
        behandlerdialog_svar_ubehandlet,
        behandlerdialog_ubesvart_ubehandlet,
        behandlerdialog_avvist_ubehandlet,
        aktivitetskrav_vurder_stans_ubehandlet,
        trenger_oppfolging,
        trenger_oppfolging_frist,
        behandler_bistand_ubehandlet,
        arbeidsuforhet_aktiv_vurdering,
        antall_sykedager,
        friskmelding_til_arbeidsformidling_fom
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        it.setObject(25, personOversiktStatus.aktivitetskravSistVurdert)
        it.setObject(26, personOversiktStatus.aktivitetskravVurderingFrist)
        it.setBoolean(27, personOversiktStatus.behandlerdialogSvarUbehandlet)
        it.setBoolean(28, personOversiktStatus.behandlerdialogUbesvartUbehandlet)
        it.setBoolean(29, personOversiktStatus.behandlerdialogAvvistUbehandlet)
        it.setBoolean(30, personOversiktStatus.aktivitetskravVurderStansUbehandlet)
        it.setBoolean(31, personOversiktStatus.trengerOppfolging)
        it.setObject(32, personOversiktStatus.trengerOppfolgingFrist)
        it.setBoolean(33, personOversiktStatus.behandlerBerOmBistandUbehandlet)
        it.setBoolean(34, personOversiktStatus.isAktivArbeidsuforhetvurdering)
        if (personOversiktStatus.latestOppfolgingstilfelle?.antallSykedager != null) {
            it.setInt(35, personOversiktStatus.latestOppfolgingstilfelle.antallSykedager)
        } else it.setNull(35, Types.INTEGER)
        it.setObject(36, personOversiktStatus.friskmeldingTilArbeidsformidlingFom)
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

const val queryUpdatePersonOversiktStatusOppfolgingstilfelle =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET oppfolgingstilfelle_updated_at = ?,
    oppfolgingstilfelle_generated_at = ?,
    oppfolgingstilfelle_start = ?,
    oppfolgingstilfelle_end = ?,
    oppfolgingstilfelle_bit_referanse_uuid = ?,
    oppfolgingstilfelle_bit_referanse_inntruffet = ?,
    sist_endret = ?,
    antall_sykedager = ?
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
        it.setObject(7, Timestamp.from(Instant.now()))
        if (oppfolgingstilfelle.antallSykedager != null) {
            it.setInt(8, oppfolgingstilfelle.antallSykedager)
        } else it.setNull(8, Types.INTEGER)
        it.setString(9, pPersonOversiktStatus.fnr)
        it.execute()
    }
    updatePersonOppfolgingstilfelleVirksomhetList(
        personOversiktStatusId = pPersonOversiktStatus.id,
        personOppfolgingstilfelleVirksomhetList = oppfolgingstilfelle.virksomhetList,
    )
}

const val updateTildeltVeilederQuery =
    """
         UPDATE PERSON_OVERSIKT_STATUS
         SET tildelt_veileder = ?, sist_endret = ?
         WHERE fnr = ?
    """

fun DatabaseInterface.lagreVeilederForBruker(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val rowCount = this.connection.use { connection ->
        connection.prepareStatement(updateTildeltVeilederQuery).use {
            it.setString(1, veilederBrukerKnytning.veilederIdent)
            it.setObject(2, Timestamp.from(Instant.now()))
            it.setString(3, veilederBrukerKnytning.fnr)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }

    if (rowCount == 0) {
        val personOversiktStatus = PersonOversiktStatus(
            veilederIdent = veilederBrukerKnytning.veilederIdent,
            fnr = veilederBrukerKnytning.fnr,
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
    SET name = ?, sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusNavn(
    personIdentNameMap: Map<String, String>,
) {
    val now = Timestamp.from(Instant.now())
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonOversiktStatusNavn).use {
            personIdentNameMap.forEach { (personident, navn) ->
                it.setString(1, navn)
                it.setObject(2, now)
                it.setString(3, personident)
                it.executeUpdate()
            }
        }
        connection.commit()
    }
}
