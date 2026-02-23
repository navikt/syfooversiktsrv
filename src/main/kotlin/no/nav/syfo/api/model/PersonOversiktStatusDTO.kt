package no.nav.syfo.api.model

import no.nav.syfo.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatDTO
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.application.meroppfolging.SenOppfolgingKandidatDTO
import no.nav.syfo.application.oppfolgingsoppgave.OppfolgingsoppgaveLatestVersionDTO
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
    val dialogmotekandidatStatus: DialogmotekandidatDTO?,
    val motestatus: String?,
    val latestOppfolgingstilfelle: PersonOppfolgingstilfelleDTO?,
    val behandlerdialogUbehandlet: Boolean,
    val behandlerBerOmBistandUbehandlet: Boolean,
    val arbeidsuforhetvurdering: ArbeidsuforhetvurderingDTO?,
    val friskmeldingTilArbeidsformidlingFom: LocalDate?,
    val oppfolgingsoppgave: OppfolgingsoppgaveLatestVersionDTO?,
    val senOppfolgingKandidat: SenOppfolgingKandidatDTO?,
    val aktivitetskravvurdering: AktivitetskravDTO?,
    val manglendeMedvirkning: ManglendeMedvirkningDTO?,
    val isAktivKartleggingssporsmalVurdering: Boolean,
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
