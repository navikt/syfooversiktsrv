package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.dialogmotekandidat.DialogmotekandidatDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.personstatus.application.meroppfolging.SenOppfolgingKandidatDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveLatestVersionDTO

data class PersonoversiktAktiveOppgaver(
    val arbeidsuforhet: ArbeidsuforhetvurderingDTO?,
    val oppfolgingsoppgave: OppfolgingsoppgaveLatestVersionDTO?,
    val aktivitetskrav: AktivitetskravDTO?,
    val manglendeMedvirkning: ManglendeMedvirkningDTO?,
    val senOppfolgingKandidat: SenOppfolgingKandidatDTO?,
    val dialogmotekandidat: DialogmotekandidatDTO?,
)
