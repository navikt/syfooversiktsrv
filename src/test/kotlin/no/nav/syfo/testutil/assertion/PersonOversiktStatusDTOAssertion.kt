package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.personstatus.api.v2.PersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.PersonOppfolgingstilfelleVirksomhetDTO
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull

fun checkPersonOppfolgingstilfelleDTO(
    personOppfolgingstilfelleDTO: PersonOppfolgingstilfelleDTO?,
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
) {
    val latestOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.firstOrNull()

    latestOppfolgingstilfelle.shouldNotBeNull()

    personOppfolgingstilfelleDTO.shouldNotBeNull()

    personOppfolgingstilfelleDTO.oppfolgingstilfelleStart shouldBeEqualTo latestOppfolgingstilfelle.start
    personOppfolgingstilfelleDTO.oppfolgingstilfelleEnd shouldBeEqualTo latestOppfolgingstilfelle.end

    checkPersonOppfolgingstilfelleVirksomhetDTOList(
        personOppfolgingstilfelleVirksomhetDTOList = personOppfolgingstilfelleDTO.virksomhetList,
        kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePerson,
    )
}

fun checkPersonOppfolgingstilfelleVirksomhetDTOList(
    personOppfolgingstilfelleVirksomhetDTOList: List<PersonOppfolgingstilfelleVirksomhetDTO>,
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
) {
    val virksomhetsnummerList = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first().virksomhetsnummerList

    personOppfolgingstilfelleVirksomhetDTOList.size shouldBeEqualTo virksomhetsnummerList.size

    personOppfolgingstilfelleVirksomhetDTOList.forEachIndexed { index, pPersonOppfolgingstilfelleVirksomhet ->
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer.value shouldBeEqualTo virksomhetsnummerList[index]
//        if (updated) {
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn shouldBeEqualTo eregOrganisasjonResponse.navn.redigertnavn
//        } else {
//            pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn.shouldBeNull()
//        }
    }
}
