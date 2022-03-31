package no.nav.syfo.testutil.generator

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.util.*

fun generateKafkaOppfolgingstilfellePerson(
    arbeidstakerAtTilfelleEnd: Boolean = true,
    oppfolgingstilfelleDurationInDays: Long = 2,
    personIdent: PersonIdent = PersonIdent(ARBEIDSTAKER_FNR),
    virksomhetsnummerList: List<Virksomhetsnummer> = listOf(
        Virksomhetsnummer(VIRKSOMHETSNUMMER)
    ),
): KafkaOppfolgingstilfellePerson {
    val start = LocalDate.now().minusDays(1)
    return KafkaOppfolgingstilfellePerson(
        uuid = UUID.randomUUID().toString(),
        createdAt = nowUTC(),
        personIdentNumber = personIdent.value,
        oppfolgingstilfelleList = listOf(
            KafkaOppfolgingstilfelle(
                arbeidstakerAtTilfelleEnd = arbeidstakerAtTilfelleEnd,
                start = start,
                end = start.plusDays(oppfolgingstilfelleDurationInDays),
                virksomhetsnummerList = virksomhetsnummerList.map { virksomhetsnummer ->
                    virksomhetsnummer.value
                },
            ),
        ),
        referanseTilfelleBitUuid = UUID.randomUUID().toString(),
        referanseTilfelleBitInntruffet = nowUTC().minusDays(1),
    )
}
