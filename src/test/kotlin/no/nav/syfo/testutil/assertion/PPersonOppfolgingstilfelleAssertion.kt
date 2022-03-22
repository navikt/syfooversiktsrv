package no.nav.syfo.testutil.assertion

import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.PPersonOppfolgingstilfelle
import org.amshove.kluent.shouldBeEqualTo

fun checkPPersonOppfolgingstilfelle(
    pPersonOppfolgingstilfelle: PPersonOppfolgingstilfelle,
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    personId: Int,
) {
    pPersonOppfolgingstilfelle.personOversiktStatusId shouldBeEqualTo personId
    pPersonOppfolgingstilfelle.virksomhetsnummer shouldBeEqualTo oversikthendelsetilfelle.virksomhetsnummer
    pPersonOppfolgingstilfelle.gradert shouldBeEqualTo oversikthendelsetilfelle.gradert
    pPersonOppfolgingstilfelle.fom shouldBeEqualTo oversikthendelsetilfelle.fom
    pPersonOppfolgingstilfelle.tom shouldBeEqualTo oversikthendelsetilfelle.tom
}
