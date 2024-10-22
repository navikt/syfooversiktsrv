package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.oppfolgingstilfelle.kafka.toPersonOppfolgingstilfelle
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleVirksomhetDTO
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
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
    personOppfolgingstilfelleDTO.varighetUker shouldBeEqualTo kafkaOppfolgingstilfellePerson.toPersonOppfolgingstilfelle(
        latestOppfolgingstilfelle
    ).varighetUker()

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

    personOppfolgingstilfelleVirksomhetDTOList.forEach { pPersonOppfolgingstilfelleVirksomhet ->
        virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer).shouldBeTrue()
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn shouldBeEqualTo eregOrganisasjonResponse.navn.redigertnavn
    }
}
