package no.nav.syfo.personstatus.api.v2

import java.time.LocalDate

data class PersonOversiktStatusDTO(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val dialogmotekandidat: Boolean?,
    val latestOppfolgingstilfelle: PersonOppfolgingstilfelleDTO?,
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
