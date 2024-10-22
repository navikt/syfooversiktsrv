package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.personstatus.domain.PPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue

fun checkPPersonOppfolgingstilfelleVirksomhet(
    pPersonOppfolgingstilfelleVirksomhetList: List<PPersonOppfolgingstilfelleVirksomhet>,
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
) {
    val virksomhetsnummerList = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first().virksomhetsnummerList

    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo virksomhetsnummerList.size

    pPersonOppfolgingstilfelleVirksomhetList.forEachIndexed { index, pPersonOppfolgingstilfelleVirksomhet ->
        virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer.value).shouldBeTrue()
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn.shouldBeNull()
    }
}

fun checkPPersonOppfolgingstilfelleVirksomhetUpdated(
    pPersonOppfolgingstilfelleVirksomhetList: List<PPersonOppfolgingstilfelleVirksomhet>,
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
) {
    val virksomhetsnummerList = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first().virksomhetsnummerList

    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo virksomhetsnummerList.size

    pPersonOppfolgingstilfelleVirksomhetList.forEach { pPersonOppfolgingstilfelleVirksomhet ->
        virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer.value).shouldBeTrue()
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn shouldBeEqualTo eregOrganisasjonResponse.navn.redigertnavn
    }
}
