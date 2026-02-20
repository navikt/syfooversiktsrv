package no.nav.syfo.application

import no.nav.syfo.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatDTO
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.application.meroppfolging.SenOppfolgingKandidatDTO
import no.nav.syfo.application.oppfolgingsoppgave.OppfolgingsoppgaveLatestVersionDTO

data class PersonoversiktAktiveOppgaver(
    val arbeidsuforhet: ArbeidsuforhetvurderingDTO?,
    val oppfolgingsoppgave: OppfolgingsoppgaveLatestVersionDTO?,
    val aktivitetskrav: AktivitetskravDTO?,
    val manglendeMedvirkning: ManglendeMedvirkningDTO?,
    val senOppfolgingKandidat: SenOppfolgingKandidatDTO?,
    val dialogmotekandidat: DialogmotekandidatDTO?,
)
