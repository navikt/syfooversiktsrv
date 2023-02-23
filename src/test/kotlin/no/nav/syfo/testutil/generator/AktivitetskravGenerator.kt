package no.nav.syfo.testutil.generator

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.domain.PersonIdent
import java.time.*

// Samme dato som i queries
private val aktivitetskravStoppunktCutoff = LocalDate.of(2023, Month.FEBRUARY, 1)

fun generateAktivitetskrav(
    personIdent: PersonIdent,
    status: AktivitetskravStatus,
    stoppunktAfterCutoff: Boolean,
    sistVurdert: OffsetDateTime? = null,
): Aktivitetskrav {
    val stoppunkt =
        if (stoppunktAfterCutoff) aktivitetskravStoppunktCutoff.plusDays(1) else aktivitetskravStoppunktCutoff.minusDays(
            1
        )
    return Aktivitetskrav(
        personIdent = personIdent,
        status = status,
        sistVurdert = sistVurdert,
        stoppunkt = stoppunkt,
    )
}
