package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingstilfelle.KafkaOppfolgingstilfelle
import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingstilfelle.OPPFOLGINGSTILFELLE_PERSON_TOPIC
import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingstilfelle.OppfolgingstilfellePersonRecord
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

fun generateKafkaOppfolgingstilfellePerson(
    arbeidstakerAtTilfelleEnd: Boolean = true,
    end: LocalDate = LocalDate.now().minusDays(1),
    oppfolgingstilfelleDurationInDays: Long = 130,
    personIdent: PersonIdent = PersonIdent(ARBEIDSTAKER_FNR),
    virksomhetsnummerList: List<Virksomhetsnummer> = listOf(
        Virksomhetsnummer(VIRKSOMHETSNUMMER)
    ),
    antallSykedager: Int? = oppfolgingstilfelleDurationInDays.toInt(),
): OppfolgingstilfellePersonRecord {
    val start = end.minusDays(oppfolgingstilfelleDurationInDays)
    return OppfolgingstilfellePersonRecord(
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
                antallSykedager = antallSykedager,
            ),
        ),
        referanseTilfelleBitUuid = UUID.randomUUID().toString(),
        referanseTilfelleBitInntruffet = nowUTC().minusDays(1).truncatedTo(ChronoUnit.MILLIS),
    )
}

fun oppfolgingstilfellePersonTopicPartition() = TopicPartition(
    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
    0
)

fun oppfolgingstilfellePersonConsumerRecord(
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) = ConsumerRecord(
    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
    0,
    1,
    "key1",
    oppfolgingstilfellePersonRecord
)
