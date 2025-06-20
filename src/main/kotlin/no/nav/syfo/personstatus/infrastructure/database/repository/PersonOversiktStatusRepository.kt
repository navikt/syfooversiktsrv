package no.nav.syfo.personstatus.infrastructure.database.repository

import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.personstatus.api.v2.model.VeilederTildelingHistorikkDTO
import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOppfolgingstilfelleVirksomhetMap
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.infrastructure.database.toList
import no.nav.syfo.util.nowUTC
import java.lang.RuntimeException
import java.sql.*
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.firstOrNull
import kotlin.use

class PersonOversiktStatusRepository(private val database: DatabaseInterface) : IPersonOversiktStatusRepository {

    override fun updateArbeidsuforhetvurderingStatus(
        personident: PersonIdent,
        isAktivVurdering: Boolean,
    ): Result<Int> {
        return try {
            database.connection.use { connection ->
                val tidspunkt = Timestamp.from(Instant.now())
                val uuid = UUID.randomUUID().toString()
                val rowsUpdated = connection.prepareStatement(UPSERT_ARBEIDSUFORHET_VURDERING_STATUS).use {
                    it.setString(1, uuid)
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
                    Result.failure(RuntimeException("Failed to update arbeidsuforhet vurdering status for personstatus: $uuid"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun upsertAktivitetskravAktivStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int> {
        return try {
            database.connection.use { connection ->
                val tidspunkt = Timestamp.from(Instant.now())
                val uuid = UUID.randomUUID().toString()
                val rowsUpdated = connection.prepareStatement(UPSERT_AKTIVITETSKRAV_VURDERING_STATUS).use {
                    it.setString(1, uuid)
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
                    Result.failure(RuntimeException("Failed to update aktivitetskrav vurdering status for personstatus: $uuid"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun upsertManglendeMedvirkningStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int> {
        return try {
            database.connection.use { connection ->
                val tidspunkt = Timestamp.from(Instant.now())
                val uuid = UUID.randomUUID().toString()
                val rowsUpdated = connection.prepareStatement(UPSERT_MANGLENDE_MEDVIRKNING_VURDERING_STATUS).use {
                    it.setString(1, uuid)
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
                    Result.failure(RuntimeException("Failed to update manglende medvirkning vurdering status for personstatus: $uuid"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun upsertSenOppfolgingKandidat(personident: PersonIdent, isAktivKandidat: Boolean): Result<Int> {
        return try {
            database.connection.use { connection ->
                val now = Timestamp.from(Instant.now())
                val uuid = UUID.randomUUID().toString()
                val rowsUpdated = connection.prepareStatement(UPSERT_PERSON_OVERSIKT_STATUS_SEN_OPPFOLGING).use {
                    it.setString(1, uuid)
                    it.setString(2, personident.value)
                    it.setBoolean(3, isAktivKandidat)
                    it.setTimestamp(4, now)
                    it.setTimestamp(5, now)
                    it.executeUpdate()
                }
                val isSuccess = rowsUpdated == 1
                if (isSuccess) {
                    connection.commit()
                    Result.success(rowsUpdated)
                } else {
                    connection.rollback()
                    Result.failure(RuntimeException("Failed to update sen oppfolging kandidat status for personstatus: $uuid"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPersonOversiktStatus(personident: PersonIdent): PersonOversiktStatus? {
        return database.connection.use { connection ->
            connection.getPersonOversiktStatus(personident)
        }
    }

    private fun Connection.getPersonOversiktStatus(personident: PersonIdent): PersonOversiktStatus? {
        val personoversiktStatus = prepareStatement(GET_PERSON_OVERSIKT_STATUS).use {
            it.setString(1, personident.value)
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
        return personoversiktStatus.firstOrNull()?.toPersonOversiktStatus()
    }

    override fun getPersonstatusesWithoutNavnOrFodselsdato(limit: Int): List<PersonOversiktStatus> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_PERSON_STATUSES_WITHOUT_NAVN_OR_FODSELSDATO).use {
                it.setInt(1, limit)
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
        }.map { it.toPersonOversiktStatus() }

    override fun createPersonOversiktStatus(personOversiktStatus: PersonOversiktStatus): PersonOversiktStatus =
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personOversiktStatus,
            )
        }

    override fun lagreVeilederForBruker(
        veilederBrukerKnytning: VeilederBrukerKnytning,
        tildeltAv: String,
    ) {
        database.connection.use { connection ->
            val existingVeilederAndEnhet = connection.getExistingVeilederAndEnhet(veilederBrukerKnytning)
            if (existingVeilederAndEnhet == null) {
                throw SQLException("lagreVeilederForBruker failed, no existing personoversiktStatus found.")
            } else if (existingVeilederAndEnhet.veileder != veilederBrukerKnytning.veilederIdent) {
                connection.updateTildeltVeileder(existingVeilederAndEnhet, veilederBrukerKnytning)
                connection.addVeilederHistorikk(existingVeilederAndEnhet, veilederBrukerKnytning, tildeltAv)
                connection.commit()
            }
        }
    }

    private fun Connection.getExistingVeilederAndEnhet(veilederBrukerKnytning: VeilederBrukerKnytning) =
        this.prepareStatement(GET_TILDELT_VEILEDER_QUERY).use {
            it.setString(1, veilederBrukerKnytning.fnr)
            it.executeQuery().toList {
                VeilederAndEnhet(
                    getInt("id"),
                    getString("tildelt_veileder"),
                    getString("tildelt_enhet")
                )
            }
        }.firstOrNull()

    private fun Connection.updateTildeltVeileder(
        existingVeilederAndEnhet: VeilederAndEnhet,
        veilederBrukerKnytning: VeilederBrukerKnytning,
    ) {
        val rowCount = this.prepareStatement(UPDATE_TILDELT_VEILEDER_QUERY).use {
            it.setString(1, veilederBrukerKnytning.veilederIdent)
            it.setObject(2, Timestamp.from(Instant.now()))
            it.setInt(3, existingVeilederAndEnhet.id)
            it.executeUpdate()
        }
        if (rowCount != 1) {
            throw SQLException("updateTildeltVeileder failed, expected single row to be updated.")
        }
    }

    private fun Connection.addVeilederHistorikk(
        existingVeilederAndEnhet: VeilederAndEnhet,
        veilederBrukerKnytning: VeilederBrukerKnytning,
        tildeltAv: String,
    ) {
        this.prepareStatement(CREATE_VEILEDER_HISTORIKK).use {
            it.setString(1, UUID.randomUUID().toString())
            it.setObject(2, OffsetDateTime.now())
            it.setInt(3, existingVeilederAndEnhet.id)
            it.setDate(4, Date.valueOf(LocalDate.now()))
            it.setString(5, veilederBrukerKnytning.veilederIdent)
            it.setString(6, existingVeilederAndEnhet.enhet)
            it.setString(7, tildeltAv)
            it.execute()
        }
    }

    override fun getVeilederTilknytningHistorikk(personident: PersonIdent): List<VeilederTildelingHistorikkDTO> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_VEILEDER_HISTORIKK).use {
                it.setString(1, personident.value)
                it.executeQuery().toList {
                    VeilederTildelingHistorikkDTO(
                        tildeltDato = getDate(1).toLocalDate(),
                        tildeltVeileder = getString(2),
                        tildeltEnhet = getString(3),
                        tildeltAv = getString(4),
                    )
                }
            }
        }

    override fun getPersonerWithOppgaveAndOldEnhet(): List<PersonIdent> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_PERSONER_WITH_OPPGAVE_AND_OLD_ENHET).use {
                it.executeQuery().toList { PersonIdent(getString("fnr")) }
            }
        }

    override fun getPersonerWithVeilederTildelingAndOldOppfolgingstilfelle(): List<PersonOversiktStatus> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_PERSONER_WITH_VEILEDERTILDELING_AND_OLD_OPPFOLGINGSTILFELLE).use {
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
        }.map { it.toPersonOversiktStatus() }

    override fun removeTildeltVeileder(personIdent: PersonIdent) {
        database.connection.use { connection ->
            connection.prepareStatement(REMOVE_TILDELT_VEILEDER).use {
                it.setString(1, personIdent.value)
                it.execute()
            }
            connection.commit()
        }
    }

    override fun removeTildeltEnhet(personIdent: PersonIdent) {
        database.connection.use { connection ->
            connection.prepareStatement(REMOVE_TILDELT_ENHET).use {
                it.setString(1, personIdent.value)
                it.execute()
            }
            connection.commit()
        }
    }

    override fun updatePersonTildeltEnhetAndRemoveTildeltVeileder(personIdent: PersonIdent, enhetId: String) {
        val now = nowUTC()
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_PERSON_TILDELT_VEILEDER_AND_ENHET).use {
                it.setNull(1, Types.NULL)
                it.setString(2, enhetId)
                it.setObject(3, now)
                it.setObject(4, now.toLocalDateTime())
                it.setString(5, personIdent.value)
                it.execute()
            }
            connection.commit()
        }
    }

    override fun updatePersonTildeltEnhetUpdatedAt(personIdent: PersonIdent) {
        val now = nowUTC()
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_PERSON_TILDELT_ENHET_UPDATED_AT).use {
                it.setObject(1, now)
                it.setObject(2, now)
                it.setString(3, personIdent.value)
                it.execute()
            }
            connection.commit()
        }
    }

    override fun updatePersonstatusesWithNavnAndFodselsdato(personer: List<PersonOversiktStatus>): List<Result<PersonOversiktStatus>> =
        personer.map { person ->
            database.connection.use { connection ->
                connection.updatePersonStatusWithNavnAndFodselsdato(person)
            }
        }

    private fun Connection.updatePersonStatusWithNavnAndFodselsdato(person: PersonOversiktStatus): Result<PersonOversiktStatus> =
        try {
            this.prepareStatement(UPDATE_PERSON_NAVN_OG_FODSELSDATO).use {
                it.setString(1, person.navn)
                if (person.fodselsdato != null) {
                    it.setDate(2, Date.valueOf(person.fodselsdato))
                } else {
                    it.setNull(2, Types.DATE)
                }
                it.setObject(3, Timestamp.from(Instant.now()))
                it.setString(4, person.fnr)
                it.executeQuery()
                    .toList { toPPersonOversiktStatus().toPersonOversiktStatus() }
                    .first()
                    .let { Result.success<PersonOversiktStatus>(it) }
                    .also { this.commit() }
            }
        } catch (e: SQLException) {
            Result.failure(e)
        }

    override fun searchPerson(search: Search): List<PersonOversiktStatus> {
        val results = when (search) {
            is Search.ByName -> searchByName(search)
            is Search.ByNameAndDate -> searchByDateAndName(search)
            is Search.ByDate -> searchByDate(search)
            is Search.ByInitialsAndDate -> searchByInitialsAndDate(search)
        }
        val personOppfolgingstilfelleVirksomhetMap = database.connection.use { connection ->
            connection.getPersonOppfolgingstilfelleVirksomhetMap(pPersonOversikStatusIds = results.map { it.id })
        }

        return results.map {
            val personOppfolgingstilfelleVirksomhetList =
                personOppfolgingstilfelleVirksomhetMap[it.id]?.toPersonOppfolgingstilfelleVirksomhet() ?: emptyList()
            it.toPersonOversiktStatus(personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelleVirksomhetList)
        }
    }

    private fun searchByName(search: Search.ByName): List<PPersonOversiktStatus> {
        val name = search.name.value.split(" ").joinToString(separator = "% ", postfix = "%")
        val nameQuery = "AND p.name ILIKE ?"
        val query = "$SEARCH_PERSON_BASE_QUERY $nameQuery $ORDER_BY_ASC"
        return database.connection.use { connection ->
            connection.prepareStatement(query).use {
                it.setString(1, name)
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
        }
    }

    private fun searchByDateAndName(search: Search.ByNameAndDate): List<PPersonOversiktStatus> {
        val name = search.name.value.split(" ").joinToString(separator = "% ", postfix = "%")
        val nameAndDateQuery = "AND p.name ILIKE ? AND p.fodselsdato = ?"
        val query = "$SEARCH_PERSON_BASE_QUERY $nameAndDateQuery $ORDER_BY_ASC"
        return database.connection.use { connection ->
            connection.prepareStatement(query).use {
                it.setString(1, name)
                it.setDate(2, Date.valueOf(search.birthdate))
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
        }
    }

    private fun searchByDate(search: Search.ByDate): List<PPersonOversiktStatus> {
        val dateQuery = "AND p.fodselsdato = ?"
        val query = "$SEARCH_PERSON_BASE_QUERY $dateQuery $ORDER_BY_ASC"
        return database.connection.use { connection ->
            connection.prepareStatement(query).use {
                it.setDate(1, Date.valueOf(search.birthdate))
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
        }
    }

    private fun searchByInitialsAndDate(search: Search.ByInitialsAndDate): List<PPersonOversiktStatus> {
        val initials = search.initials.value.toList().joinToString(separator = "% ", postfix = "%")
        val initialsAndDateQuery = "AND p.name ILIKE ? AND p.fodselsdato = ?"
        val query = "$SEARCH_PERSON_BASE_QUERY $initialsAndDateQuery $ORDER_BY_ASC"
        return database.connection.use { connection ->
            connection.prepareStatement(query).use {
                it.setString(1, initials)
                it.setDate(2, Date.valueOf(search.birthdate))
                it.executeQuery().toList { toPPersonOversiktStatus() }
            }
        }
    }

    override fun updatePersonOversiktStatusOppfolgingstilfelle(
        personstatus: PersonOversiktStatus,
        oppfolgingstilfelle: Oppfolgingstilfelle,
    ) {
        database.connection.use { connection ->
            val personstatusId = connection.prepareStatement(UPDATE_PERSON_OPPFOLGINGSTILFELLE).use {
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
                it.setString(9, personstatus.fnr)
                it.executeQuery().toList { getInt("id") }.first()
            }
            connection.updatePersonOppfolgingstilfelleVirksomhetList(
                personOversiktStatusId = personstatusId,
                personOppfolgingstilfelleVirksomhetList = oppfolgingstilfelle.virksomhetList,
            )
            connection.commit()
        }
    }

    override fun updateOppfolgingsoppgave(personIdent: PersonIdent, isActiveOppfolgingsoppgave: Boolean): Result<Int> {
        return try {
            database.connection.use { connection ->
                val rowsAffected = connection.prepareStatement(UPDATE_PERSON_OPPFOLGINGSOPPGAVE).use {
                    it.setBoolean(1, isActiveOppfolgingsoppgave)
                    it.setTimestamp(2, Timestamp.from(Instant.now()))
                    it.setString(3, personIdent.value)
                    it.executeUpdate()
                }
                if (rowsAffected == 1) {
                    connection.commit()
                    Result.success(rowsAffected)
                } else {
                    connection.rollback()
                    Result.failure(RuntimeException("updateOppfolgingsoppgave failed, expected single row to be updated."))
                }
            }
        } catch (e: SQLException) {
            Result.failure(e)
        }
    }

    companion object {
        private const val GET_PERSON_OVERSIKT_STATUS =
            """
            SELECT *
            FROM PERSON_OVERSIKT_STATUS
            WHERE fnr = ?
            """

        private const val UPSERT_ARBEIDSUFORHET_VURDERING_STATUS =
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

        private const val UPSERT_PERSON_OVERSIKT_STATUS_SEN_OPPFOLGING =
            """
            INSERT INTO person_oversikt_status (
                id,
                uuid,
                fnr,
                is_aktiv_sen_oppfolging_kandidat,
                opprettet,
                sist_endret
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?)
            ON CONFLICT (fnr)
            DO UPDATE SET
                is_aktiv_sen_oppfolging_kandidat = EXCLUDED.is_aktiv_sen_oppfolging_kandidat,
                sist_endret = EXCLUDED.sist_endret
            """

        private const val UPSERT_AKTIVITETSKRAV_VURDERING_STATUS =
            """
            INSERT INTO person_oversikt_status (
                id,
                uuid,
                fnr,
                is_aktiv_aktivitetskrav_vurdering,
                opprettet,
                sist_endret
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?)
            ON CONFLICT (fnr)
            DO UPDATE SET
                is_aktiv_aktivitetskrav_vurdering = EXCLUDED.is_aktiv_aktivitetskrav_vurdering,
                sist_endret = EXCLUDED.sist_endret
            """

        private const val UPSERT_MANGLENDE_MEDVIRKNING_VURDERING_STATUS =
            """
            INSERT INTO person_oversikt_status (
                id,
                uuid,
                fnr,
                is_aktiv_manglende_medvirkning_vurdering,
                opprettet,
                sist_endret
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?)
            ON CONFLICT (fnr)
            DO UPDATE SET
                is_aktiv_manglende_medvirkning_vurdering = EXCLUDED.is_aktiv_manglende_medvirkning_vurdering,
                sist_endret = EXCLUDED.sist_endret
            """

        private const val GET_TILDELT_VEILEDER_QUERY =
            """
            SELECT id,tildelt_veileder,tildelt_enhet FROM PERSON_OVERSIKT_STATUS
            WHERE fnr = ?
            """

        private const val UPDATE_TILDELT_VEILEDER_QUERY =
            """
            UPDATE PERSON_OVERSIKT_STATUS
            SET tildelt_veileder = ?, sist_endret = ?
            WHERE id = ?
            """

        private const val CREATE_VEILEDER_HISTORIKK =
            """
            INSERT INTO VEILEDER_HISTORIKK (
                id,uuid,created_at,person_oversikt_status_id,tildelt_dato,tildelt_veileder,tildelt_enhet,tildelt_av
            ) VALUES(DEFAULT,?,?,?,?,?,?,?)
            """

        private const val GET_VEILEDER_HISTORIKK =
            """
            SELECT tildelt_dato,tildelt_veileder,tildelt_enhet,tildelt_av 
            FROM VEILEDER_HISTORIKK
            WHERE person_oversikt_status_id IN (select id from person_oversikt_status where fnr=?)
            ORDER BY tildelt_dato DESC
            """

        const val AKTIV_OPPGAVE_WHERE_CLAUSE =
            """
        (
        motebehov_ubehandlet = 't' 
        OR oppfolgingsplan_lps_bistand_ubehandlet = 't' 
        OR dialogmotekandidat = 't' 
        OR dialogmotesvar_ubehandlet = 't'
        OR behandlerdialog_svar_ubehandlet = 't'
        OR behandlerdialog_ubesvart_ubehandlet = 't'
        OR behandlerdialog_avvist_ubehandlet = 't'
        OR trenger_oppfolging = 't'
        OR behandler_bistand_ubehandlet = 't'
        OR friskmelding_til_arbeidsformidling_fom IS NOT NULL
        OR arbeidsuforhet_aktiv_vurdering = 't'
        OR is_aktiv_sen_oppfolging_kandidat = 't'
        OR is_aktiv_aktivitetskrav_vurdering = 't'
        OR is_aktiv_manglende_medvirkning_vurdering = 't'
        )
        """

        private const val GET_PERSONER_WITH_OPPGAVE_AND_OLD_ENHET =
            """
            SELECT fnr
            FROM PERSON_OVERSIKT_STATUS
            WHERE $AKTIV_OPPGAVE_WHERE_CLAUSE    
            AND (tildelt_enhet_updated_at IS NULL OR tildelt_enhet_updated_at <= NOW() - INTERVAL '24 HOURS')
            ORDER BY tildelt_enhet_updated_at ASC
            LIMIT 2000
            """

        private const val UPDATE_PERSON_TILDELT_VEILEDER_AND_ENHET =
            """
            UPDATE PERSON_OVERSIKT_STATUS
            SET tildelt_veileder = ?, tildelt_enhet = ?, tildelt_enhet_updated_at = ?, sist_endret = ?
            WHERE fnr = ?
            """

        private const val UPDATE_PERSON_TILDELT_ENHET_UPDATED_AT =
            """
            UPDATE PERSON_OVERSIKT_STATUS
            SET tildelt_enhet_updated_at = ?, sist_endret = ?
            WHERE fnr = ?
            """

        private const val REMOVE_TILDELT_VEILEDER =
            """
            UPDATE person_oversikt_status
            SET tildelt_veileder = NULL
            WHERE fnr = ?
            """

        private const val REMOVE_TILDELT_ENHET =
            """
            UPDATE person_oversikt_status
            SET tildelt_enhet = NULL
            WHERE fnr = ?
            """

        private const val GET_PERSONER_WITH_VEILEDERTILDELING_AND_OLD_OPPFOLGINGSTILFELLE =
            """
            SELECT *
            FROM person_oversikt_status
            WHERE (tildelt_veileder IS NOT NULL OR tildelt_enhet IS NOT NULL)
            AND oppfolgingstilfelle_end + INTERVAL '2 MONTH' < now()
            AND (sist_endret IS NULL OR sist_endret + INTERVAL '2 MONTH' < now())
            LIMIT 8000;
            """

        private const val GET_PERSON_STATUSES_WITHOUT_NAVN_OR_FODSELSDATO =
            """
            SELECT * 
            FROM PERSON_OVERSIKT_STATUS
            WHERE (name IS NULL OR fodselsdato IS NULL) AND (oppfolgingstilfelle_end + INTERVAL '16 DAY' >= now() OR $AKTIV_OPPGAVE_WHERE_CLAUSE)
            LIMIT ?
            """

        private const val UPDATE_PERSON_NAVN_OG_FODSELSDATO =
            """
            UPDATE PERSON_OVERSIKT_STATUS
            SET name = ?, fodselsdato = ?, sist_endret = ?
            WHERE fnr = ?
            RETURNING *
            """

        private const val UPDATE_PERSON_OPPFOLGINGSTILFELLE =
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
            RETURNING id
            """

        private const val UPDATE_PERSON_OPPFOLGINGSOPPGAVE =
            """
                UPDATE PERSON_OVERSIKT_STATUS
                SET trenger_oppfolging = ?, sist_endret = ?
                WHERE fnr = ?
            """

        private const val SEARCH_PERSON_BASE_QUERY =
            "SELECT * FROM PERSON_OVERSIKT_STATUS p WHERE (p.oppfolgingstilfelle_end + INTERVAL '16 DAY' >= now() OR $AKTIV_OPPGAVE_WHERE_CLAUSE)"

        private const val ORDER_BY_ASC = "ORDER BY name ASC"
    }
}

private data class VeilederAndEnhet(
    val id: Int,
    val veileder: String?,
    val enhet: String?,
)

private fun ResultSet.toPPersonOversiktStatus(): PPersonOversiktStatus =
    PPersonOversiktStatus(
        id = getInt("id"),
        uuid = getString("uuid").let { UUID.fromString(it) },
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        fodselsdato = getObject("fodselsdato", LocalDate::class.java),
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
        behandlerdialogSvarUbehandlet = getBoolean("behandlerdialog_svar_ubehandlet"),
        behandlerdialogUbesvartUbehandlet = getBoolean("behandlerdialog_ubesvart_ubehandlet"),
        behandlerdialogAvvistUbehandlet = getBoolean("behandlerdialog_avvist_ubehandlet"),
        isAktivOppfolgingsoppgave = getBoolean("trenger_oppfolging"),
        behandlerBerOmBistandUbehandlet = getBoolean("behandler_bistand_ubehandlet"),
        antallSykedager = getObject("antall_sykedager") as Int?,
        isAktivArbeidsuforhetvurdering = getBoolean("arbeidsuforhet_aktiv_vurdering"),
        friskmeldingTilArbeidsformidlingFom = getObject("friskmelding_til_arbeidsformidling_fom", LocalDate::class.java),
        isAktivSenOppfolgingKandidat = getBoolean("is_aktiv_sen_oppfolging_kandidat"),
        isAktivAktivitetskravvurdering = getBoolean("is_aktiv_aktivitetskrav_vurdering"),
        isAktivManglendeMedvirkningVurdering = getBoolean("is_aktiv_manglende_medvirkning_vurdering"),
    )
