package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.OppfolgingstilfellePersonRecord
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull

fun checkPPersonOversiktStatusOppfolgingstilfelle(
    pPersonOversiktStatus: PPersonOversiktStatus,
    oppfolgingstilfellePersonRecord: OppfolgingstilfellePersonRecord,
) {
    val latestOppfolgingstilfelle = oppfolgingstilfellePersonRecord.oppfolgingstilfelleList.firstOrNull()

    latestOppfolgingstilfelle.shouldNotBeNull()

    pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.toInstant()
        ?.toEpochMilli() shouldBeEqualTo oppfolgingstilfellePersonRecord.createdAt.toInstant().toEpochMilli()
    pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.offset shouldBeEqualTo oppfolgingstilfellePersonRecord.createdAt.offset
    pPersonOversiktStatus.oppfolgingstilfelleUpdatedAt.shouldNotBeNull()
    pPersonOversiktStatus.oppfolgingstilfelleStart shouldBeEqualTo latestOppfolgingstilfelle.start
    pPersonOversiktStatus.oppfolgingstilfelleEnd shouldBeEqualTo latestOppfolgingstilfelle.end
    pPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.toInstant()
        ?.toEpochMilli() shouldBeEqualTo oppfolgingstilfellePersonRecord.referanseTilfelleBitInntruffet.toInstant()
        .toEpochMilli()
    pPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.offset shouldBeEqualTo oppfolgingstilfellePersonRecord.referanseTilfelleBitInntruffet.offset
    pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.toString() shouldBeEqualTo oppfolgingstilfellePersonRecord.referanseTilfelleBitUuid
    pPersonOversiktStatus.antallSykedager shouldBeEqualTo latestOppfolgingstilfelle.antallSykedager
}
