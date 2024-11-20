package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.OppfolgingstilfellePersonRecord
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleVirksomhetDTO
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull

fun checkPersonOppfolgingstilfelleDTO(
    personOppfolgingstilfelleDTO: PersonOppfolgingstilfelleDTO?,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val latestOppfolgingstilfelle = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.firstOrNull()

    latestOppfolgingstilfelle.shouldNotBeNull()

    personOppfolgingstilfelleDTO.shouldNotBeNull()

    personOppfolgingstilfelleDTO.oppfolgingstilfelleStart shouldBeEqualTo latestOppfolgingstilfelle.start
    personOppfolgingstilfelleDTO.oppfolgingstilfelleEnd shouldBeEqualTo latestOppfolgingstilfelle.end
    personOppfolgingstilfelleDTO.varighetUker shouldBeEqualTo oppfolgingstilfellePersonRecord.toPersonOppfolgingstilfelle(
        latestOppfolgingstilfelle
    ).varighetUker()

    checkPersonOppfolgingstilfelleVirksomhetDTOList(
        personOppfolgingstilfelleVirksomhetDTOList = personOppfolgingstilfelleDTO.virksomhetList,
        oppfolgingstilfellePersonRecord = oppfolgingstilfellePersonRecord,
    )
}

fun checkPersonOppfolgingstilfelleVirksomhetDTOList(
    personOppfolgingstilfelleVirksomhetDTOList: List<PersonOppfolgingstilfelleVirksomhetDTO>,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val virksomhetsnummerList = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.first().virksomhetsnummerList

    personOppfolgingstilfelleVirksomhetDTOList.size shouldBeEqualTo virksomhetsnummerList.size

    personOppfolgingstilfelleVirksomhetDTOList.forEach { pPersonOppfolgingstilfelleVirksomhet ->
        virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer).shouldBeTrue()
        pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn shouldBeEqualTo eregOrganisasjonResponse.navn.redigertnavn
    }
}
