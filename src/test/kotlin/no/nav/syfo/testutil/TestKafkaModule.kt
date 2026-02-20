package no.nav.syfo.testutil

import io.mockk.mockk
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.infrastructure.database.TransactionManager
import no.nav.syfo.infrastructure.kafka.dialogmotekandidat.KafkaDialogmotekandidatEndring
import no.nav.syfo.infrastructure.kafka.dialogmotekandidat.DialogmotekandidatEndringConsumer
import no.nav.syfo.infrastructure.kafka.dialogmotestatusendring.DialogmoteStatusendringConsumer
import no.nav.syfo.infrastructure.kafka.frisktilarbeid.FriskTilArbeidVedtakConsumer
import no.nav.syfo.infrastructure.kafka.frisktilarbeid.VedtakStatusRecord
import no.nav.syfo.infrastructure.kafka.oppfolgingstilfelle.OppfolgingstilfellePersonRecord
import no.nav.syfo.infrastructure.kafka.oppfolgingstilfelle.OppfolgingstilfelleConsumer
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import org.apache.kafka.clients.consumer.KafkaConsumer

object TestKafkaModule {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val transactionManager = TransactionManager(database)

    val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer(
        oppfolgingstilfelleService = externalMockEnvironment.oppfolgingstilfelleService
    )
    val kafkaConsumerOppfolgingstilfellePerson = mockk<KafkaConsumer<String, OppfolgingstilfellePersonRecord>>()

    val kafkaPersonoppgavehendelse = mockk<KafkaConsumer<String, KPersonoppgavehendelse>>()

    val dialogmotekandidatEndringConsumer = DialogmotekandidatEndringConsumer(
        transactionManager = transactionManager,
        personoversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    )
    val kafkaConsumerDialogmotekandidatEndring = mockk<KafkaConsumer<String, KafkaDialogmotekandidatEndring>>()

    val dialogmoteStatusendringConsumer = DialogmoteStatusendringConsumer(
        transactionManager = transactionManager,
        personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    )
    val kafkaConsumerDialogmoteStatusendring = mockk<KafkaConsumer<String, KDialogmoteStatusEndring>>()

    val friskTilArbeidVedtakConsumer = FriskTilArbeidVedtakConsumer(
        transactionManager = transactionManager,
        personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    )
    val kafkaConsumerFriskTilArbeid = mockk<KafkaConsumer<String, VedtakStatusRecord>>()
}
