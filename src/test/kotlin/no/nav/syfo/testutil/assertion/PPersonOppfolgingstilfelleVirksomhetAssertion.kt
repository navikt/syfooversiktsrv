package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.personstatus.domain.PPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull

fun checkPPersonOppfolgingstilfelleVirksomhet(
    pPersonOppfolgingstilfelleVirksomhetList: List<PPersonOppfolgingstilfelleVirksomhet>,
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
    updated: Boolean,
) {
    val virksomhetsnummerList = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first().virksomhetsnummerList

    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo virksomhetsnummerList.size

    pPersonOppfolgingstilfelleVirksomhetList.forEachIndexed { index, pPersonOppfolgingstilfelleVirksomhet ->
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer.value shouldBeEqualTo virksomhetsnummerList[index]
        if (updated) {
            pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn shouldBeEqualTo eregOrganisasjonResponse.navn.redigertnavn
        } else {
            pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn.shouldBeNull()
        }
    }
}
