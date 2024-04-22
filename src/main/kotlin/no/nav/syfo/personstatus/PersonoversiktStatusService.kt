package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.oppfolgingstilfelle.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personoppgavehendelse.kafka.*
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID

class PersonoversiktStatusService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    private val isUbehandlet = true
    private val isBehandlet = false

    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String, arenaCutoff: LocalDate): List<PersonOversiktStatus> {
        val personListe = database.hentUbehandledePersonerTilknyttetEnhet(
            enhet = enhet,
        )
        return personListe.map { pPersonOversikStatus ->
            val personOppfolgingstilfelleVirksomhetList = getPersonOppfolgingstilfelleVirksomhetList(
                pPersonOversikStatusId = pPersonOversikStatus.id,
            )
            pPersonOversikStatus.toPersonOversiktStatus(
                personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelleVirksomhetList,
            )
        }.filter { personOversiktStatus ->
            personOversiktStatus.hasActiveOppgave(arenaCutoff = arenaCutoff)
        }
    }

    private fun getPersonOppfolgingstilfelleVirksomhetList(
        pPersonOversikStatusId: Int,
    ): List<PersonOppfolgingstilfelleVirksomhet> =
        database.connection.use { connection ->
            connection.getPersonOppfolgingstilfelleVirksomhetList(
                pPersonOversikStatusId = pPersonOversikStatusId,
            ).toPersonOppfolgingstilfelleVirksomhet()
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
                val callId = UUID.randomUUID().toString()
                val personident = PersonIdent(personoppgavehendelse.personident)
                val hendelsetype = personoppgavehendelse.hendelsetype

                createOrUpdatePersonOversiktStatus(
                    connection = connection,
                    personident = personident,
                    oversikthendelseType = hendelsetype,
                    callId = callId,
                )

                log.info("Finished processing record with callId: $callId")
                COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ.increment()
            }
            connection.commit()
        }
    }

    private fun createOrUpdatePersonOversiktStatus(
        connection: Connection,
        personident: PersonIdent,
        oversikthendelseType: OversikthendelseType,
        callId: String,
    ) {
        val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
            fnr = personident.value,
        ).firstOrNull()

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = PersonOversiktStatus(fnr = personident.value)
            val personOversiktStatusWithHendelseType = personOversiktStatus.applyHendelse(oversikthendelseType)

            log.info("TRACE: No existing status for person, callId: $callId")
            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatusWithHendelseType,
            )
            COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATED_PERSONOVERSIKT_STATUS.increment()
        } else {
            log.info("TRACE: Found existing PersonOversiktStatus, oversikthendelseType: $oversikthendelseType callId: $callId")
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
                OversikthendelseType.ARBEIDSUFORHET_VURDER_AVSLAG_MOTTATT -> connection.updateArbeidsuforhetVurderAvslag(isUbehandlet, personident)
                OversikthendelseType.ARBEIDSUFORHET_VURDER_AVSLAG_BEHANDLET -> connection.updateArbeidsuforhetVurderAvslag(isBehandlet, personident)
            }

            COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS.increment()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonoversiktStatusService::class.java)
    }
}
