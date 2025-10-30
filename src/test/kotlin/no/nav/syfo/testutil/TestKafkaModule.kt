package no.nav.syfo.testutil

import io.mockk.mockk
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat.KafkaDialogmotekandidatEndring
import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat.KafkaDialogmotekandidatEndringService
import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotestatusendring.KafkaDialogmoteStatusendringService
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.FriskTilArbeidVedtakConsumer
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.VedtakStatusRecord
import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingstilfelle.OppfolgingstilfellePersonRecord
import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingstilfelle.OppfolgingstilfelleConsumer
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import org.apache.kafka.clients.consumer.KafkaConsumer

object TestKafkaModule {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer(
        oppfolgingstilfelleService = externalMockEnvironment.oppfolgingstilfelleService
    )
    val kafkaConsumerOppfolgingstilfellePerson = mockk<KafkaConsumer<String, OppfolgingstilfellePersonRecord>>()

    val kafkaPersonoppgavehendelse = mockk<KafkaConsumer<String, KPersonoppgavehendelse>>()

    val kafkaDialogmotekandidatEndringService = KafkaDialogmotekandidatEndringService(
        database = database
    )
    val kafkaConsumerDialogmotekandidatEndring = mockk<KafkaConsumer<String, KafkaDialogmotekandidatEndring>>()

    val kafkaDialogmoteStatusendringService = KafkaDialogmoteStatusendringService(
        database = database
    )
    val kafkaConsumerDialogmoteStatusendring = mockk<KafkaConsumer<String, KDialogmoteStatusEndring>>()

    val friskTilArbeidVedtakConsumer = FriskTilArbeidVedtakConsumer(
        database = database,
    )
    val kafkaConsumerFriskTilArbeid = mockk<KafkaConsumer<String, VedtakStatusRecord>>()
}
