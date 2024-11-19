package no.nav.syfo.personstatus.api.v2.model

import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.personstatus.application.meroppfolging.SenOppfolgingKandidatDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveDTO
import java.time.LocalDate

data class PersonOversiktStatusDTO(
    val veilederIdent: String?,
    val fnr: String,
    val fodselsdato: LocalDate?,
    val navn: String,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val dialogmotesvarUbehandlet: Boolean,
    val dialogmotekandidat: Boolean?,
    val motestatus: String?,
    val latestOppfolgingstilfelle: PersonOppfolgingstilfelleDTO?,
    val behandlerdialogUbehandlet: Boolean,
    val behandlerBerOmBistandUbehandlet: Boolean,
    val arbeidsuforhetvurdering: ArbeidsuforhetvurderingDTO?,
    val friskmeldingTilArbeidsformidlingFom: LocalDate?,
    val oppfolgingsoppgave: OppfolgingsoppgaveDTO?,
    val senOppfolgingKandidat: SenOppfolgingKandidatDTO?,
    val aktivitetskravvurdering: AktivitetskravDTO?,
    val manglendeMedvirkning: ManglendeMedvirkningDTO?,
)

data class PersonOppfolgingstilfelleDTO(
    val oppfolgingstilfelleStart: LocalDate,
    val oppfolgingstilfelleEnd: LocalDate,
    val varighetUker: Int,
    val virksomhetList: List<PersonOppfolgingstilfelleVirksomhetDTO>,
)

data class PersonOppfolgingstilfelleVirksomhetDTO(
    val virksomhetsnummer: String,
    val virksomhetsnavn: String?,
)
