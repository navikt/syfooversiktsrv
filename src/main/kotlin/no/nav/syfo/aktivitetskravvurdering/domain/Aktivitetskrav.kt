package no.nav.syfo.aktivitetskravvurdering.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class Aktivitetskrav(
    val personIdent: PersonIdent,
    val status: AktivitetskravVurderingStatus,
    val updatedAt: OffsetDateTime,
    val stoppunkt: LocalDate,
)

enum class AktivitetskravVurderingStatus {
    NY,
    AVVENT,
    UNNTAK,
    OPPFYLT,
    AUTOMATISK_OPPFYLT,
    STANS,
}

fun Aktivitetskrav.toPersonOversiktStatus() = PersonOversiktStatus(
    veilederIdent = null,
    fnr = this.personIdent.value,
    navn = null,
    enhet = null,
    motebehovUbehandlet = null,
    oppfolgingsplanLPSBistandUbehandlet = null,
    dialogmotesvarUbehandlet = false,
    dialogmotekandidat = null,
    dialogmotekandidatGeneratedAt = null,
    motestatus = null,
    motestatusGeneratedAt = null,
    latestOppfolgingstilfelle = null,
    aktivitetskrav = this.status,
    aktivitetskravStoppunkt = this.stoppunkt,
    aktivitetskravUpdatedAt = this.updatedAt,
)
