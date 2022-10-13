package no.nav.syfo.testutil.generator

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDate
import java.util.*

fun generateKafkaOppfolgingstilfellePerson(
    arbeidstakerAtTilfelleEnd: Boolean = true,
    end: LocalDate = LocalDate.now().minusDays(1),
    oppfolgingstilfelleDurationInDays: Long = 130,
    personIdent: PersonIdent = PersonIdent(ARBEIDSTAKER_FNR),
    virksomhetsnummerList: List<Virksomhetsnummer> = listOf(
        Virksomhetsnummer(VIRKSOMHETSNUMMER)
    ),
): KafkaOppfolgingstilfellePerson {
    val start = end.minusDays(oppfolgingstilfelleDurationInDays)
    return KafkaOppfolgingstilfellePerson(
        uuid = UUID.randomUUID().toString(),
        createdAt = nowUTC(),
        personIdentNumber = personIdent.value,
        oppfolgingstilfelleList = listOf(
            KafkaOppfolgingstilfelle(
                arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
                start = start,
                end = end,
                virksomhetsnummerList = virksomhetsnummerList.map { virksomhetsnummer ->
                    virksomhetsnummer.value
                },
            ),
        ),
        referanseTilfelleBitUuid = UUID.randomUUID().toString(),
        referanseTilfelleBitInntruffet = nowUTC().minusDays(1),
    )
}

fun oppfolgingstilfellePersonTopicPartition() = TopicPartition(
    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
    0
)

fun oppfolgingstilfellePersonConsumerRecord(
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
) = ConsumerRecord(
    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
    0,
    1,
    "key1",
    kafkaOppfolgingstilfellePerson
)
