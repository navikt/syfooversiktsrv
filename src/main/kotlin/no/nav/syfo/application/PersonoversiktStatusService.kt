package no.nav.syfo.application

import no.nav.syfo.domain.OversikthendelseType
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.domain.addPersonName
import no.nav.syfo.domain.filterHasActiveOppgave
import no.nav.syfo.infrastructure.clients.pdl.model.fodselsdato
import no.nav.syfo.infrastructure.clients.pdl.model.fullName
import no.nav.syfo.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.infrastructure.database.queries.updateBehandlerBerOmBistand
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktMotebehov
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusBehandlerdialogAvvist
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusBehandlerdialogSvar
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusBehandlerdialogUbesvart
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusDialogmotesvar
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusLPS
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATED_PERSONOVERSIKT_STATUS
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import java.sql.Connection

class PersonoversiktStatusService(
    private val pdlClient: IPdlClient,
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
    private val transactionManager: ITransactionManager,
) {
    private val isUbehandlet = true
    private val isBehandlet = false

    fun hentPersonoversiktStatusTilknyttetEnhet(
        enhet: String,
    ): List<PersonOversiktStatus> =
        personoversiktStatusRepository.hentUbehandledePersonerTilknyttetEnhet(enhet)
            .filterHasActiveOppgave()

    fun getPersonstatus(personident: PersonIdent): PersonOversiktStatus? =
        personoversiktStatusRepository.getPersonOversiktStatus(personident)

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
        transactionManager.transaction { connection ->
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

    fun upsertKartleggingssporsmalKandidatStatus(personident: PersonIdent, isAktivKandidat: Boolean): Result<Int> {
        return personoversiktStatusRepository.upsertKartleggingssporsmalKandidatStatus(
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
        val existingPersonOversiktStatus =
            personoversiktStatusRepository.getPersonOversiktStatus(personident, connection)

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = PersonOversiktStatus(fnr = personident.value)
            val personOversiktStatusWithHendelseType = personOversiktStatus.applyOversikthendelse(oversikthendelseType)

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
