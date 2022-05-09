package no.nav.syfo.testutil

import io.mockk.mockk
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndringService
import no.nav.syfo.dialogmotestatusendring.kafka.KafkaDialogmoteStatusendringService
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePersonService
import no.nav.syfo.personstatus.kafka.KafkaOversiktHendelseService
import org.apache.kafka.clients.consumer.KafkaConsumer

object TestKafkaModule {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    val kafkaOppfolgingstilfellePersonService = KafkaOppfolgingstilfellePersonService(
        database = database,
    )
    val kafkaConsumerOppfolgingstilfellePerson = mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePerson>>()

    val kafkaOversiktHendelseService = KafkaOversiktHendelseService(
        database = database,
    )

    val kafkaDialogmotekandidatEndringService = KafkaDialogmotekandidatEndringService(
        database = database
    )
    val kafkaConsumerDialogmotekandidatEndring = mockk<KafkaConsumer<String, KafkaDialogmotekandidatEndring>>()

    val kafkaDialogmoteStatusendringService = KafkaDialogmoteStatusendringService()
    val kafkaConsumerDialogmoteStatusendring = mockk<KafkaConsumer<String, KDialogmoteStatusEndring>>()
}
