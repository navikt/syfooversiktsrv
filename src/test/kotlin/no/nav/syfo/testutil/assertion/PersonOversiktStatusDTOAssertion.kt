package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.OppfolgingstilfellePersonRecord
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleVirksomhetDTO
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNotNull

fun checkPersonOppfolgingstilfelleDTO(
    personOppfolgingstilfelleDTO: PersonOppfolgingstilfelleDTO?,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val latestOppfolgingstilfelle = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.firstOrNull()

    assertNotNull(latestOppfolgingstilfelle)

    assertNotNull(personOppfolgingstilfelleDTO)

    assertEquals(personOppfolgingstilfelleDTO.oppfolgingstilfelleStart, latestOppfolgingstilfelle.start)
    assertEquals(personOppfolgingstilfelleDTO.oppfolgingstilfelleEnd, latestOppfolgingstilfelle.end)
    assertEquals(
        personOppfolgingstilfelleDTO.varighetUker,
        oppfolgingstilfellePersonRecord.toPersonOppfolgingstilfelle(
            latestOppfolgingstilfelle
        ).varighetUker()
    )

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

    assertEquals(personOppfolgingstilfelleVirksomhetDTOList.size, virksomhetsnummerList.size)

    personOppfolgingstilfelleVirksomhetDTOList.forEach { pPersonOppfolgingstilfelleVirksomhet ->
        assertTrue(virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer))
        assertEquals(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn, eregOrganisasjonResponse.navn.redigertnavn)
    }
}
