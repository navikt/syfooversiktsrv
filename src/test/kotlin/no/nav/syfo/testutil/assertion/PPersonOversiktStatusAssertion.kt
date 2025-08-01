package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.OppfolgingstilfellePersonRecord
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNotNull

fun checkPPersonOversiktStatusOppfolgingstilfelle(
    pPersonOversiktStatus: PPersonOversiktStatus,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val latestOppfolgingstilfelle = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.firstOrNull()

    assertNotNull(latestOppfolgingstilfelle)

    assertEquals(
        pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.toInstant()
            ?.toEpochMilli(),
        oppfolgingstilfellePersonRecord.createdAt.toInstant().toEpochMilli()
    )
    assertEquals(
        pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.offset,
        oppfolgingstilfellePersonRecord.createdAt.offset
    )
    assertNotNull(pPersonOversiktStatus.oppfolgingstilfelleUpdatedAt)
    assertEquals(pPersonOversiktStatus.oppfolgingstilfelleStart, latestOppfolgingstilfelle.start)
    assertEquals(pPersonOversiktStatus.oppfolgingstilfelleEnd, latestOppfolgingstilfelle.end)
    assertEquals(
        pPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.toInstant()
            ?.toEpochMilli(),
        oppfolgingstilfellePersonRecord.referanseTilfelleBitInntruffet.toInstant()
            .toEpochMilli()
    )
    assertEquals(
        pPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.offset,
        oppfolgingstilfellePersonRecord.referanseTilfelleBitInntruffet.offset
    )
    assertEquals(
        pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.toString(),
        oppfolgingstilfellePersonRecord.referanseTilfelleBitUuid
    )
    assertEquals(pPersonOversiktStatus.antallSykedager, latestOppfolgingstilfelle.antallSykedager)
}
