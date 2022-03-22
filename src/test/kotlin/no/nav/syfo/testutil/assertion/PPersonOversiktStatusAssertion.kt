package no.nav.syfo.testutil.assertion

import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull

fun checkPPersonOversiktStatus(
    pPersonOversiktStatus: PPersonOversiktStatus,
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    veilederIdent: String?,
) {
    pPersonOversiktStatus.fnr shouldBeEqualTo oversikthendelsetilfelle.fnr
    pPersonOversiktStatus.navn shouldBeEqualTo null
    pPersonOversiktStatus.veilederIdent shouldBeEqualTo veilederIdent
    pPersonOversiktStatus.enhet shouldBeEqualTo oversikthendelsetilfelle.enhetId
    pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
    pPersonOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
    pPersonOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
}
