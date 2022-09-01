package no.nav.syfo.personstatus.kafka

import io.netty.util.internal.StringUtil
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.metric.*
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.kafkaCallId
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaOversiktHendelseService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<KOversikthendelse> {

    override val pollDurationInMillis: Long = 100

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KOversikthendelse>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        processRecords(records)
    }

    private fun processRecords(consumerRecords: ConsumerRecords<String, KOversikthendelse>) {
        var logValues = arrayOf(
            StructuredArguments.keyValue("oversikthendelseId", "missing"),
            StructuredArguments.keyValue("Harfnr", "missing"),
            StructuredArguments.keyValue("hendelseId", "missing")
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        consumerRecords.forEach {
            val callId = kafkaCallId()
            val oversiktHendelse: KOversikthendelse = it.value()
            logValues = arrayOf(
                StructuredArguments.keyValue("oversikthendelseId", it.key()),
                StructuredArguments.keyValue("harFnr", (!StringUtil.isNullOrEmpty(oversiktHendelse.fnr)).toString()),
                StructuredArguments.keyValue("hendelseId", oversiktHendelse.hendelseId)
            )
            log.info("Mottatt oversikthendelse, klar for oppdatering, $logKeys, {}", *logValues, callIdArgument(callId))
            try {
                oppdaterPersonMedHendelse(oversiktHendelse, callId)
            } catch (sqlException: PSQLException) {
                // retry once before giving up (could be database concurrency conflict)
                oppdaterPersonMedHendelse(oversiktHendelse, callId)
            }
        }
    }

    fun oppdaterPersonMedHendelse(
        oversikthendelse: KOversikthendelse,
        callId: String = "",
    ) {
        val oversikthendelseType = oversikthendelse.oversikthendelseType()

        if (oversikthendelseType == null) {
            handleMissingOversikthendelseType(
                hendelseId = oversikthendelse.hendelseId,
                callId = callId,
            )
        } else {
            oppdaterPerson(
                oversikthendelse = oversikthendelse,
                oversikthendelseType = oversikthendelseType,
                callId = callId,
            )
        }
    }

    private fun handleMissingOversikthendelseType(
        hendelseId: String,
        callId: String = "",
    ) {
        log.error(
            "Mottatt oversikthendelse med ukjent type, $hendelseId, {}",
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT.increment()
    }

    private fun oppdaterPerson(
        oversikthendelse: KOversikthendelse,
        oversikthendelseType: OversikthendelseType,
        callId: String = "",
    ) {
        val personOversiktStatus = database.getPersonOversiktStatusList(
            fnr = oversikthendelse.fnr,
        ).firstOrNull()?.toPersonOversiktStatus(
            personOppfolgingstilfelleVirksomhetList = emptyList(),
        )

        if (personOversiktStatus == null) {
            createNewOversiktstatus(
                oversikthendelse = oversikthendelse,
                oversikthendelseType = oversikthendelseType,
                callId = callId,
            )
        } else {
            updatePersonOversiktStatus(
                oversikthendelseType = oversikthendelseType,
                personOversiktStatus = personOversiktStatus,
            )
        }
    }

    private fun createNewOversiktstatus(
        oversikthendelse: KOversikthendelse,
        oversikthendelseType: OversikthendelseType,
        callId: String = "",
    ) {
        if (oversikthendelseType.isNotBehandling()) {
            createPersonOversiktStatus(
                oversiktHendelse = oversikthendelse,
                oversikthendelseType = oversikthendelseType,
            )
        } else {
            log.error(
                "Fant ikke person som skal oppdateres med hendelse {}, {}",
                oversikthendelse.hendelseId,
                callIdArgument(callId)
            )
            countFailed(
                oversikthendelseType = oversikthendelseType,
            )
        }
    }

    private fun createPersonOversiktStatus(
        oversiktHendelse: KOversikthendelse,
        oversikthendelseType: OversikthendelseType,
    ) {
        val personOversiktStatus = oversiktHendelse.toPersonOversiktStatus(
            oversikthendelseType = oversikthendelseType,
        )
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personOversiktStatus,
            )
        }
        countCreated(
            oversikthendelseType = oversikthendelseType,
        )
    }

    private fun updatePersonOversiktStatus(
        oversikthendelseType: OversikthendelseType,
        personOversiktStatus: PersonOversiktStatus,
    ) {
        val updatedPersonOversiktStatus =
            personOversiktStatus.applyHendelse(oversikthendelseType = oversikthendelseType)
        database.updatePersonOversiktStatus(
            personOversiktStatus = updatedPersonOversiktStatus,
        )
        countUpdated(
            oversikthendelseType = oversikthendelseType,
        )
    }

    private fun countCreated(
        oversikthendelseType: OversikthendelseType,
    ) {
        when (oversikthendelseType) {
            OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPRETT.increment()
            OversikthendelseType.DIALOGMOTESVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_DIALOGMOTESVAR_MOTTATT_OPPRETT.increment()
            else -> return
        }
    }

    private fun countFailed(
        oversikthendelseType: OversikthendelseType,
    ) {
        when (oversikthendelseType) {
            OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_FEILET.increment()
            OversikthendelseType.DIALOGMOTESVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_DIALOGMOTESVAR_BEHANDLET_FEILET.increment()
            else -> return
        }
    }

    private fun countUpdated(
        oversikthendelseType: OversikthendelseType,
    ) {
        when (oversikthendelseType) {
            OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER.increment()
            OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPDATER.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_OPPDATER.increment()
            OversikthendelseType.DIALOGMOTESVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_DIALOGMOTESVAR_MOTTATT_OPPDATER.increment()
            OversikthendelseType.DIALOGMOTESVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_DIALOGMOTESVAR_BEHANDLET_OPPDATER.increment()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaOversiktHendelseService::class.java)
    }
}
