package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveDTO

data class PersonoversiktAktiveOppgaver(
    val arbeidsuforhet: ArbeidsuforhetvurderingDTO?,
    val oppfolgingsoppgave: OppfolgingsoppgaveDTO?,
    val aktivitetskrav: AktivitetskravDTO?,
    val manglendeMedvirkning: ManglendeMedvirkningDTO?,
)
