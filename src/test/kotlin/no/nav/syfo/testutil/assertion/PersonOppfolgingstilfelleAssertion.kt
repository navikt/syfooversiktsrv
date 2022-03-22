package no.nav.syfo.testutil.assertion

import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.api.v2.OppfolgingstilfelleDTO
import org.amshove.kluent.shouldBeEqualTo

fun checkPersonOppfolgingstilfelle(
    oppfolgingstilfelle: OppfolgingstilfelleDTO,
    oversikthendelsetilfelle: KOversikthendelsetilfelle
) {
    oppfolgingstilfelle.virksomhetsnummer shouldBeEqualTo oversikthendelsetilfelle.virksomhetsnummer
    oppfolgingstilfelle.fom shouldBeEqualTo oversikthendelsetilfelle.fom
    oppfolgingstilfelle.tom shouldBeEqualTo oversikthendelsetilfelle.tom
}
