package no.nav.syfo.personstatus.api.v2

import java.time.LocalDate
import java.time.LocalDateTime

data class PersonOversiktStatusDTO(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val dialogmotesvarUbehandlet: Boolean,
    val dialogmotekandidat: Boolean?,
    val motestatus: String?,
    val latestOppfolgingstilfelle: PersonOppfolgingstilfelleDTO?,
    val aktivitetskrav: String?,
    val aktivitetskravStoppunkt: LocalDate?,
    val aktivitetskravSistVurdert: LocalDateTime?,
)

data class PersonOppfolgingstilfelleDTO(
    val oppfolgingstilfelleStart: LocalDate,
    val oppfolgingstilfelleEnd: LocalDate,
    val virksomhetList: List<PersonOppfolgingstilfelleVirksomhetDTO>,
)

data class PersonOppfolgingstilfelleVirksomhetDTO(
    val virksomhetsnummer: String,
    val virksomhetsnavn: String?,
)
