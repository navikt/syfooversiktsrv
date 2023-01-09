package no.nav.syfo.personoppgavehendelse.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.kafkaCallId
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration

class PersonoppgavehendelseService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<KPersonoppgavehendelse> {
    override val pollDurationInMillis: Long = 100
    val LPS_BISTAND_UBEHANDLET_TRUE = true
    val LPS_BISTAND_UBEHANDLET_FALSE = false
    val DIALOGMOTESVAR_UBEHANDLET_TRUE = true
    val DIALOGMOTESVAR_UBEHANDLET_FALSE = false

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KPersonoppgavehendelse>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))

        if (records.count() > 0) {
            log.info("TRACE: Received ${records.count()} records")
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, KPersonoppgavehendelse>) {

        // TODO: vurdere å legge inn støtte for tombstoned records

        database.connection.use { connection ->
            records.forEach { record ->
                val callId = kafkaCallId()
                val personoppgaveHendelse = record.value()
                try {
                    processPersonoppgavehendelse(
                        connection,
                        personoppgaveHendelse,
                        callId,
                    )
                } catch (sqlException: SQLException) {
                    // retry once before giving up (could be database concurrency conflict)
                    log.info("Got sqlException, try again, callId: $callId")
                    processPersonoppgavehendelse(
                        connection,
                        personoppgaveHendelse,
                        callId,
                    )
                }
                log.info("TRACE: Finished processing record! callId: $callId")
                COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ.increment()
            }

            connection.commit()
        }
    }

    fun processPersonoppgavehendelse(
        connection: Connection,
        kPersonoppgavehendelse: KPersonoppgavehendelse,
        callId: String,
    ) {
        val oversikthendelseType = kPersonoppgavehendelse.oversikthendelseType()
        if (oversikthendelseType == null) {
            handleMissingPersonoppgavehendelseType(
                hendelsetype = kPersonoppgavehendelse.hendelsetype,
                callId = callId,
            )
        } else {
            val personident = PersonIdent(kPersonoppgavehendelse.personident)

            log.info("TRACE: Found valid hendelsetype $oversikthendelseType, callId: $callId")
            createOrUpdatePersonOversiktStatus(
                connection = connection,
                personident = personident,
                oversikthendelseType = oversikthendelseType,
                callId = callId,
            )
        }
    }

    private fun handleMissingPersonoppgavehendelseType(
        hendelsetype: String,
        callId: String = "",
    ) {
        log.error(
            "Received personoppgavehendelse with unknown or missing hendelsetype, $hendelsetype, {}",
            callIdArgument(callId)
        )
        COUNT_PERSONOPPGAVEHENDELSE_UKJENT_MOTTATT.increment()
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
                    connection.updatePersonOversiktStatusLPS(LPS_BISTAND_UBEHANDLET_TRUE, personident)
                OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET ->
                    connection.updatePersonOversiktStatusLPS(LPS_BISTAND_UBEHANDLET_FALSE, personident)
                OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT ->
                    connection.updatePersonOversiktMotebehov(true, personident)
                OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET ->
                    connection.updatePersonOversiktMotebehov(false, personident)
                OversikthendelseType.DIALOGMOTESVAR_MOTTATT ->
                    connection.updatePersonOversiktStatusDialogmotesvar(DIALOGMOTESVAR_UBEHANDLET_TRUE, personident)
                OversikthendelseType.DIALOGMOTESVAR_BEHANDLET ->
                    connection.updatePersonOversiktStatusDialogmotesvar(DIALOGMOTESVAR_UBEHANDLET_FALSE, personident)
            }

            COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS.increment()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonoppgavehendelseService::class.java)
    }
}
