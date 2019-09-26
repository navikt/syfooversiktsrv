package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.LOG
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelleV2
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.hentPersonResultatInternal
import no.nav.syfo.util.CallIdArgument

class OversikthendelstilfelleService(private val database: DatabaseInterface) {

    fun oppdaterPersonMedHendelse(
            oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2,
            callId: String = ""
    ) {
        val person = database.hentPersonResultatInternal(oversikthendelsetilfelleV2.fnr)

        when {
            person.isEmpty() -> {
                database.opprettPersonOppfolgingstilfelleMottatt(oversikthendelsetilfelleV2)
                countOpprett(oversikthendelsetilfelleV2, callId)
            }
            erPersonsEnhetOppdatert(person, oversikthendelsetilfelleV2.enhetId) -> {
                database.oppdaterPersonOppfolgingstilfelleNyEnhetMottatt(person.first().id, oversikthendelsetilfelleV2)
                countOppdaterNyEnhet(oversikthendelsetilfelleV2, callId)
            }
            else -> {
                database.oppdaterPersonOppfolgingstilfelleMottatt(person.first().id, oversikthendelsetilfelleV2)
                countOppdater(oversikthendelsetilfelleV2, callId)
            }
        }
    }
    companion object {

        fun erPersonsEnhetOppdatert(person: List<PPersonOversiktStatus>, nyEnhetId: String): Boolean {
            val enhet = person.first().enhet
            return nyEnhetId.isNotEmpty() && nyEnhetId != enhet
        }
    }
}

fun countOpprett(
        oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2,
        callId: String = ""
) {
    if (oversikthendelsetilfelleV2.gradert) {
        LOG.info("Opprettet person basert pa gradert oversikthendelsetilfelleV2, for enhet {}, {}", oversikthendelsetilfelleV2.enhetId, CallIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT.inc()
    } else {
        LOG.info("Opprettet person basert pa oversikthendelsetilfelleV2 med ingen aktivitet, for enhet {}, {}", oversikthendelsetilfelleV2.enhetId, CallIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT.inc()
    }
}

fun countOppdaterNyEnhet(
        oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2,
        callId: String = ""
) {
    if (oversikthendelsetilfelleV2.gradert) {
        LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelleV2 mottatt med ny enhet, {}", CallIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET.inc()
    } else {
        LOG.info("Oppdatert person basert pa oversikthendelsetilfelleV2 med ingen aktivitet mottatt med ny enhet, {}", CallIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER_ENHET.inc()
    }
}

fun countOppdater(
        oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2,
        callId: String = ""
) {
    if (oversikthendelsetilfelleV2.gradert) {
        LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelleV2 mottatt, {}", CallIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER.inc()
    } else {
        LOG.info("Oppdatert person basert pa oversikthendelsetilfelleV2 med ingen aktivitet mottatt, {}", CallIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER.inc()
    }
}
