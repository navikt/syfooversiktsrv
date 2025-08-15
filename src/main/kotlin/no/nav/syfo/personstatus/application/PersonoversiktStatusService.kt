package no.nav.syfo.personstatus.application

import no.nav.syfo.oppfolgingstilfelle.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATED_PERSONOVERSIKT_STATUS
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOppfolgingstilfelleVirksomhetMap
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.domain.addPersonName
import no.nav.syfo.personstatus.domain.applyHendelse
import no.nav.syfo.personstatus.domain.hasActiveOppgave
import no.nav.syfo.personstatus.domain.toPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personstatus.domain.toPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fodselsdato
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fullName
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.database.queries.hentUbehandledePersonerTilknyttetEnhet
import no.nav.syfo.personstatus.infrastructure.database.queries.updateBehandlerBerOmBistand
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktMotebehov
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusBehandlerdialogAvvist
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusBehandlerdialogSvar
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusBehandlerdialogUbesvart
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusDialogmotesvar
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusLPS
import java.sql.Connection
import kotlin.collections.associateBy
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.mapValues
import kotlin.text.isNullOrEmpty
import kotlin.use

class PersonoversiktStatusService(
    private val database: DatabaseInterface,
    private val pdlClient: IPdlClient,
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    private val isUbehandlet = true
    private val isBehandlet = false

    fun hentPersonoversiktStatusTilknyttetEnhet(
        enhet: String,
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
            personOversiktStatus.hasActiveOppgave()
        }
    }

    fun getPersonstatus(personident: PersonIdent): PersonOversiktStatus? =
        personoversiktStatusRepository.getPersonOversiktStatus(personident)

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
            personOversiktStatusList.addPersonName(personIdentNameMap = personIdentNavnMap)
        }
    }

    fun createOrUpdatePersonoversiktStatuser(personoppgavehendelser: List<KPersonoppgavehendelse>) {
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

    fun upsertManglendeMedvirkningStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int> =
        personoversiktStatusRepository.upsertManglendeMedvirkningStatus(
            personident = personident,
            isAktivVurdering = isAktivVurdering,
        )

    suspend fun updateNavnOrFodselsdatoWhereMissing(updateLimit: Int): List<Result<PersonOversiktStatus>> {
        val personStatuser = personoversiktStatusRepository.getPersonstatusesWithoutNavnOrFodselsdato(updateLimit)
        if (personStatuser.isEmpty()) {
            return emptyList()
        }
        val personidenter = personStatuser.map { PersonIdent(it.fnr) }
        val pdlPersons = pdlClient.getPersons(personidenter = personidenter)?.hentPersonBolk ?: emptyList()
        val pdlPersonsById = pdlPersons.associateBy { it.ident }
        val editedPersonStatuser = personStatuser.mapNotNull { personStatus ->
            val pdlPerson = pdlPersonsById[personStatus.fnr]
            val fullName = pdlPerson?.person?.fullName()
            val fodselsdato = pdlPerson?.person?.fodselsdato()

            val isUpdate = fullName != null || fodselsdato != null
            if (isUpdate) {
                personStatus.updatePersonDetails(navn = fullName, fodselsdato = fodselsdato)
            } else {
                null
            }
        }
        return personoversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(editedPersonStatuser)
    }

    fun getPersonerWithVeilederOrEnhetTildelingAndOldOppfolgingstilfelle(): List<PersonOversiktStatus> =
        personoversiktStatusRepository.getPersonerWithVeilederTildelingAndOldOppfolgingstilfelle()

    fun removeTildeltVeileder(personIdent: PersonIdent) =
        personoversiktStatusRepository.removeTildeltVeileder(personIdent)

    fun removeTildeltEnhet(personIdent: PersonIdent) =
        personoversiktStatusRepository.removeTildeltEnhet(personIdent)

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
                OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT ->
                    connection.updateBehandlerBerOmBistand(isUbehandlet, personident)
                OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET ->
                    connection.updateBehandlerBerOmBistand(isBehandlet, personident)
            }

            COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS.increment()
        }
    }
}
