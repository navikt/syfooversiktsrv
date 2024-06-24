package no.nav.syfo.personstatus.infrastructure.database.repository

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.toPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.toList
import java.lang.RuntimeException
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.firstOrNull
import kotlin.use

class PersonOversiktStatusRepository(private val database: DatabaseInterface) : IPersonOversiktStatusRepository {

    override fun updateArbeidsuforhetvurderingStatus(
        personident: PersonIdent,
        isAktivVurdering: Boolean
    ): Result<Int> {
        return try {
            database.connection.use { connection ->
                val tidspunkt = Timestamp.from(Instant.now())
                val rowsUpdated = connection.prepareStatement(UPDATE_OR_INSERT_PERSON_OVERSIKT_STATUS).use {
                    it.setString(1, UUID.randomUUID().toString())
                    it.setString(2, personident.value)
                    it.setBoolean(3, isAktivVurdering)
                    it.setTimestamp(4, tidspunkt)
                    it.setTimestamp(5, tidspunkt)
                    it.executeUpdate()
                }
                val isSuccess = rowsUpdated == 1
                if (isSuccess) {
                    connection.commit()
                    Result.success(rowsUpdated)
                } else {
                    connection.rollback()
                    Result.failure(RuntimeException("Failed to update arbeidsuforhet vurdering status for person with fnr: ${personident.value}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPersonOversiktStatus(personident: PersonIdent): PersonOversiktStatus? {
        database.connection.use { connection ->
            val personoversiktStatus = connection.prepareStatement(GET_PERSON_OVERSIKT_STATUS).use {
                it.setString(1, personident.value)
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
            return personoversiktStatus.firstOrNull()?.toPersonOversiktStatus()
        }
    }

    companion object {
        private const val GET_PERSON_OVERSIKT_STATUS =
            """
            SELECT *
            FROM PERSON_OVERSIKT_STATUS
            WHERE fnr = ?
            """

        private const val UPDATE_OR_INSERT_PERSON_OVERSIKT_STATUS =
            """
            INSERT INTO person_oversikt_status (
                id,
                uuid,
                fnr,
                arbeidsuforhet_aktiv_vurdering,
                opprettet,
                sist_endret
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?)
            ON CONFLICT (fnr)
            DO UPDATE SET
                arbeidsuforhet_aktiv_vurdering = EXCLUDED.arbeidsuforhet_aktiv_vurdering,
                sist_endret = EXCLUDED.sist_endret
            """
    }
}

private fun ResultSet.toPPersonOversiktStatus(): PPersonOversiktStatus =
    PPersonOversiktStatus(
        id = getInt("id"),
        uuid = getString("uuid").let { UUID.fromString(it) },
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        navn = getString("name"),
        enhet = getString("tildelt_enhet"),
        tildeltEnhetUpdatedAt = getObject("tildelt_enhet_updated_at", OffsetDateTime::class.java),
        motebehovUbehandlet = getObject("motebehov_ubehandlet") as Boolean?,
        oppfolgingsplanLPSBistandUbehandlet = getObject("oppfolgingsplan_lps_bistand_ubehandlet") as Boolean?,
        dialogmotesvarUbehandlet = getObject("dialogmotesvar_ubehandlet") as Boolean,
        dialogmotekandidat = getObject("dialogmotekandidat") as Boolean?,
        dialogmotekandidatGeneratedAt = getObject("dialogmotekandidat_generated_at", OffsetDateTime::class.java),
        motestatus = getString("motestatus"),
        motestatusGeneratedAt = getObject("motestatus_generated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleUpdatedAt = getObject("oppfolgingstilfelle_updated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleGeneratedAt = getObject("oppfolgingstilfelle_generated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleStart = getObject("oppfolgingstilfelle_start", LocalDate::class.java),
        oppfolgingstilfelleEnd = getObject("oppfolgingstilfelle_end", LocalDate::class.java),
        oppfolgingstilfelleBitReferanseUuid = getString("oppfolgingstilfelle_bit_referanse_uuid")
            ?.let { UUID.fromString(it) },
        oppfolgingstilfelleBitReferanseInntruffet = getObject(
            "oppfolgingstilfelle_bit_referanse_inntruffet",
            OffsetDateTime::class.java
        ),
        aktivitetskrav = getString("aktivitetskrav"),
        aktivitetskravStoppunkt = getObject("aktivitetskrav_stoppunkt", LocalDate::class.java),
        aktivitetskravUpdatedAt = getObject("aktivitetskrav_sist_vurdert", OffsetDateTime::class.java),
        aktivitetskravVurderingFrist = getObject("aktivitetskrav_vurdering_frist", LocalDate::class.java),
        behandlerdialogSvarUbehandlet = getBoolean("behandlerdialog_svar_ubehandlet"),
        behandlerdialogUbesvartUbehandlet = getBoolean("behandlerdialog_ubesvart_ubehandlet"),
        behandlerdialogAvvistUbehandlet = getBoolean("behandlerdialog_avvist_ubehandlet"),
        aktivitetskravVurderStansUbehandlet = getBoolean("aktivitetskrav_vurder_stans_ubehandlet"),
        trengerOppfolging = getBoolean("trenger_oppfolging"),
        behandlerBerOmBistandUbehandlet = getBoolean("behandler_bistand_ubehandlet"),
        antallSykedager = getObject("antall_sykedager") as Int?,
        isAktivArbeidsuforhetvurdering = getBoolean("arbeidsuforhet_aktiv_vurdering"),
        friskmeldingTilArbeidsformidlingFom = getObject("friskmelding_til_arbeidsformidling_fom", LocalDate::class.java),
    )
