package no.nav.syfo.testutil.assertion

import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingstilfelle.OppfolgingstilfellePersonRecord
import no.nav.syfo.personstatus.domain.PPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.mock.eregOrganisasjonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull

fun checkPPersonOppfolgingstilfelleVirksomhet(
    pPersonOppfolgingstilfelleVirksomhetList: List<PPersonOppfolgingstilfelleVirksomhet>,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val virksomhetsnummerList = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.first().virksomhetsnummerList

    assertEquals(pPersonOppfolgingstilfelleVirksomhetList.size, virksomhetsnummerList.size)

    pPersonOppfolgingstilfelleVirksomhetList.forEachIndexed { index, pPersonOppfolgingstilfelleVirksomhet ->
        assertTrue(virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer.value))
        assertNull(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn)
    }
}

fun checkPPersonOppfolgingstilfelleVirksomhetUpdated(
    pPersonOppfolgingstilfelleVirksomhetList: List<PPersonOppfolgingstilfelleVirksomhet>,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val virksomhetsnummerList = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.first().virksomhetsnummerList

    assertEquals(pPersonOppfolgingstilfelleVirksomhetList.size, virksomhetsnummerList.size)

    pPersonOppfolgingstilfelleVirksomhetList.forEach { pPersonOppfolgingstilfelleVirksomhet ->
        assertTrue(virksomhetsnummerList.contains(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnummer.value))
        assertEquals(pPersonOppfolgingstilfelleVirksomhet.virksomhetsnavn, eregOrganisasjonResponse.navn.redigertnavn)
    }
}
