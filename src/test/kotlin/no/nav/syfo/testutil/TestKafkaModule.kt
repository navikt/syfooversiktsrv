package no.nav.syfo.testutil

import io.mockk.mockk
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndringService
import no.nav.syfo.dialogmotestatusendring.kafka.KafkaDialogmoteStatusendringService
import no.nav.syfo.frisktilarbeid.kafka.KafkaFriskTilArbeidService
import no.nav.syfo.frisktilarbeid.kafka.VedtakStatusRecord
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePersonService
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.infrastructure.kafka.ArbeidsuforhetvurderingRecord
import org.apache.kafka.clients.consumer.KafkaConsumer

object TestKafkaModule {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    val kafkaOppfolgingstilfellePersonService = KafkaOppfolgingstilfellePersonService(
        database = database,
    )
    val kafkaConsumerOppfolgingstilfellePerson = mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePerson>>()

    val kafkaPersonoppgavehendelse = mockk<KafkaConsumer<String, KPersonoppgavehendelse>>()

    val kafkaDialogmotekandidatEndringService = KafkaDialogmotekandidatEndringService(
        database = database
    )
    val kafkaConsumerDialogmotekandidatEndring = mockk<KafkaConsumer<String, KafkaDialogmotekandidatEndring>>()

    val kafkaDialogmoteStatusendringService = KafkaDialogmoteStatusendringService(
        database = database
    )
    val kafkaConsumerDialogmoteStatusendring = mockk<KafkaConsumer<String, KDialogmoteStatusEndring>>()

    val kafkaFriskTilArbeidService = KafkaFriskTilArbeidService(
        database = database,
    )
    val kafkaConsumerFriskTilArbeid = mockk<KafkaConsumer<String, VedtakStatusRecord>>()
    val arbeidsuforhetVurderingConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetvurderingRecord>>()
}
