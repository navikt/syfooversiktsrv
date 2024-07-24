package no.nav.syfo.personstatus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.oppfolgingstilfelle.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personoppgavehendelse.kafka.*
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.application.GetAktivitetskravForPersonsResponseDTO
import no.nav.syfo.personstatus.application.IAktivitetskravClient
import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingerResponseDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.IArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.IOppfolgingsoppgaveClient
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaverResponseDTO
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.LocalDate

class PersonoversiktStatusService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
    private val arbeidsuforhetvurderingClient: IArbeidsuforhetvurderingClient,
    private val aktivitetskravClient: IAktivitetskravClient,
    private val oppfolgingsoppgaveClient: IOppfolgingsoppgaveClient,
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    private val isUbehandlet = true
    private val isBehandlet = false
    private val log = LoggerFactory.getLogger(PersonoversiktStatusService::class.java)

    fun hentPersonoversiktStatusTilknyttetEnhet(
        enhet: String,
        arenaCutoff: LocalDate,
    ): List<PersonOversiktStatus> {
        val pPersonOversiktStatuser = database.hentUbehandledePersonerTilknyttetEnhet(enhet)
        val personOppfolgingstilfelleVirksomhetMap = getPersonOppfolgingstilfelleVirksomhetMap(
            pPersonOversikStatusIds = pPersonOversiktStatuser.map { it.id }
        )

        return pPersonOversiktStatuser.map {
            val personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelleVirksomhetMap[it.id]
                ?: emptyList()
            it.toPersonOversiktStatus(personOppfolgingstilfelleVirksomhetList)
        }.filter { personOversiktStatus ->
            personOversiktStatus.hasActiveOppgave(arenaCutoff = arenaCutoff) // TODO: Trenger vi denne når db-spørringen henter ut bare aktive personer?
        }
    }

    fun getPersonstatus(personident: PersonIdent): PersonOversiktStatus? =
        personoversiktStatusRepository.getPersonOversiktStatus(personident)

    suspend fun getAktiveVurderinger(
        callId: String,
        token: String,
        arenaCutoff: LocalDate,
        personStatusOversikt: List<PersonOversiktStatus>,
    ): List<PersonOversiktStatusDTO> {
        val activeOppfolgingsoppgaver = getActiveOppfolgingsoppgaver(
            callId = callId,
            token = token,
            personStatuser = personStatusOversikt,
        )
        val arbeidsuforhetvurderinger = getArbeidsuforhetvurderinger(
            callId = callId,
            token = token,
            personStatuser = personStatusOversikt,
        )
        val activeAktivitetskrav = getActiveAktivitetskravForPersons(
            callId = callId,
            token = token,
            personStatuser = personStatusOversikt,
        )

        return personStatusOversikt.map { personStatus ->
            personStatus.toPersonOversiktStatusDTO(
                arenaCutoff = arenaCutoff,
                arbeidsuforhetvurdering = arbeidsuforhetvurderinger.await()
                    ?.vurderinger
                    ?.get(personStatus.fnr),
                oppfolgingsoppgave = activeOppfolgingsoppgaver.await()
                    ?.oppfolgingsoppgaver
                    ?.get(personStatus.fnr),
                aktivitetskravvurdering = activeAktivitetskrav.await()
                    ?.aktivitetskravvurderinger
                    ?.get(personStatus.fnr),
            )
        }
    }

    private suspend fun getArbeidsuforhetvurderinger(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<ArbeidsuforhetvurderingerResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithArbeidsuforhetvurdering = personStatuser
                .filter { it.isAktivArbeidsuforhetvurdering }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithArbeidsuforhetvurdering.isNotEmpty()) {
                arbeidsuforhetvurderingClient.getLatestVurderinger(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithArbeidsuforhetvurdering,
                )
            } else {
                null
            }
        }

    private suspend fun getActiveOppfolgingsoppgaver(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<OppfolgingsoppgaverResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithOppfolgingsoppgave = personStatuser
                .filter { it.trengerOppfolging }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithOppfolgingsoppgave.isNotEmpty()) {
                val response = oppfolgingsoppgaveClient.getActiveOppfolgingsoppgaver(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithOppfolgingsoppgave,
                )
                if (response == null) {
                    log.error("Oppfolgingsoppgaver was null for enhet ${personStatuser[0].enhet}")
                }
                response
            } else {
                null
            }
        }

    private suspend fun getActiveAktivitetskravForPersons(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<GetAktivitetskravForPersonsResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithActiveAktivitetskrav = personStatuser
                .filter { it.isAktivAktivitetskravvurdering }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithActiveAktivitetskrav.isNotEmpty()) {
                aktivitetskravClient.getAktivitetskravForPersons(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithActiveAktivitetskrav,
                )
            } else {
                null
            }
        }

    private fun getPersonOppfolgingstilfelleVirksomhetMap(
        pPersonOversikStatusIds: List<Int>,
    ): Map<Int, List<PersonOppfolgingstilfelleVirksomhet>> =
        database.connection.use { connection ->
            connection.getPersonOppfolgingstilfelleVirksomhetMap(
                pPersonOversikStatusIds = pPersonOversikStatusIds,
            ).mapValues { it.value.toPersonOppfolgingstilfelleVirksomhet() }
        }

    suspend fun getPersonOversiktStatusListWithName(
        callId: String,
        personOversiktStatusList: List<PersonOversiktStatus>,
    ): List<PersonOversiktStatus> {
        val personIdentMissingNameList = personOversiktStatusList
            .filter { it.navn.isNullOrEmpty() }
            .map { personOversiktStatus ->
                PersonIdent(personOversiktStatus.fnr)
            }

        return if (personIdentMissingNameList.isEmpty()) {
            personOversiktStatusList
        } else {
            val personIdentNavnMap = pdlClient.getPdlPersonIdentNumberNavnMap(
                callId = callId,
                personIdentList = personIdentMissingNameList,
            )
            database.updatePersonOversiktStatusNavn(personIdentNavnMap)
            personOversiktStatusList.addPersonName(personIdentNameMap = personIdentNavnMap)
        }
    }

    fun createOrUpdatePersonoversiktStatuser(
        personoppgavehendelser: List<KPersonoppgavehendelse>,
    ) {
        database.connection.use { connection ->
            personoppgavehendelser.forEach { personoppgavehendelse ->
                val personident = PersonIdent(personoppgavehendelse.personident)
                val hendelsetype = personoppgavehendelse.hendelsetype

                createOrUpdatePersonOversiktStatus(
                    connection = connection,
                    personident = personident,
                    oversikthendelseType = hendelsetype,
                )
                COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ.increment()
            }
            connection.commit()
        }
    }

    fun updateArbeidsuforhetvurderingStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int> {
        return personoversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
            personident = personident,
            isAktivVurdering = isAktivVurdering
        )
    }

    fun upsertSenOppfolgingKandidat(personident: PersonIdent, isAktivKandidat: Boolean): Result<Int> {
        return personoversiktStatusRepository.upsertSenOppfolgingKandidat(
            personident = personident,
            isAktivKandidat = isAktivKandidat,
        )
    }

    fun upsertAktivitetskravvurderingStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int> =
        personoversiktStatusRepository.upsertAktivitetskravAktivStatus(
            personident = personident,
            isAktivVurdering = isAktivVurdering
        )

    private fun createOrUpdatePersonOversiktStatus(
        connection: Connection,
        personident: PersonIdent,
        oversikthendelseType: OversikthendelseType,
    ) {
        val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
            fnr = personident.value,
        ).firstOrNull()

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = PersonOversiktStatus(fnr = personident.value)
            val personOversiktStatusWithHendelseType = personOversiktStatus.applyHendelse(oversikthendelseType)

            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatusWithHendelseType,
            )
            COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATED_PERSONOVERSIKT_STATUS.increment()
        } else {
            when (oversikthendelseType) {
                OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT ->
                    connection.updatePersonOversiktStatusLPS(isUbehandlet, personident)
                OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET ->
                    connection.updatePersonOversiktStatusLPS(isBehandlet, personident)
                OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT ->
                    connection.updatePersonOversiktMotebehov(isUbehandlet, personident)
                OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET ->
                    connection.updatePersonOversiktMotebehov(isBehandlet, personident)
                OversikthendelseType.DIALOGMOTESVAR_MOTTATT ->
                    connection.updatePersonOversiktStatusDialogmotesvar(isUbehandlet, personident)
                OversikthendelseType.DIALOGMOTESVAR_BEHANDLET ->
                    connection.updatePersonOversiktStatusDialogmotesvar(isBehandlet, personident)
                OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT ->
                    connection.updatePersonOversiktStatusBehandlerdialogSvar(isUbehandlet, personident)
                OversikthendelseType.BEHANDLERDIALOG_SVAR_BEHANDLET ->
                    connection.updatePersonOversiktStatusBehandlerdialogSvar(isBehandlet, personident)
                OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT ->
                    connection.updatePersonOversiktStatusBehandlerdialogUbesvart(isUbehandlet, personident)
                OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET ->
                    connection.updatePersonOversiktStatusBehandlerdialogUbesvart(isBehandlet, personident)
                OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT ->
                    connection.updatePersonOversiktStatusBehandlerdialogAvvist(isUbehandlet, personident)
                OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET ->
                    connection.updatePersonOversiktStatusBehandlerdialogAvvist(isBehandlet, personident)
                OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_MOTTATT ->
                    connection.updateAktivitetskravVurderStans(isUbehandlet, personident)
                OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_BEHANDLET ->
                    connection.updateAktivitetskravVurderStans(isBehandlet, personident)
                OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT ->
                    connection.updateBehandlerBerOmBistand(isUbehandlet, personident)
                OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET ->
                    connection.updateBehandlerBerOmBistand(isBehandlet, personident)
            }

            COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS.increment()
        }
    }
}
