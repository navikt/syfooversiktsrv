package no.nav.syfo.testutil.generator

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.domain.PersonIdent
import java.time.*

class AktivitetskravGenerator(
    private val arenaCutoff: LocalDate,
) {
    fun generateAktivitetskrav(
        personIdent: PersonIdent,
        status: AktivitetskravStatus,
        stoppunktAfterCutoff: Boolean,
        sistVurdert: OffsetDateTime? = null,
    ): Aktivitetskrav {
        val stoppunkt =
            if (stoppunktAfterCutoff) arenaCutoff.plusDays(1) else arenaCutoff.minusDays(
                1
            )
        return Aktivitetskrav(
            personIdent = personIdent,
            status = status,
            sistVurdert = sistVurdert,
            stoppunkt = stoppunkt,
        )
    }
}
