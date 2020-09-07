package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.LOG
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.hentPersonResultatInternal
import no.nav.syfo.util.callIdArgument

class OversikthendelstilfelleService(private val database: DatabaseInterface) {

    fun oppdaterPersonMedHendelse(
        oversikthendelsetilfelle: KOversikthendelsetilfelle,
        callId: String = ""
    ) {
        if (isInvalidOversikthendelsetilfelle(oversikthendelsetilfelle)) {
            LOG.error("Oppdaterte ikke person med oversikthendelsetilfelle med ugyldig EnhetId, for enhet {}, {}", oversikthendelsetilfelle.enhetId, callIdArgument(callId))
            COUNT_OVERSIKTHENDELSETILFELLE_UGDYLGIG_MOTTATT.inc()
            return
        } else {
            val person = database.hentPersonResultatInternal(oversikthendelsetilfelle.fnr)

            when {
                person.isEmpty() -> {
                    database.opprettPersonOppfolgingstilfelleMottatt(oversikthendelsetilfelle)
                    countOpprett(oversikthendelsetilfelle, callId)
                }
                erPersonsEnhetOppdatert(person, oversikthendelsetilfelle.enhetId) -> {
                    database.oppdaterPersonOppfolgingstilfelleNyEnhetMottatt(person.first().id, oversikthendelsetilfelle)
                    countOppdaterNyEnhet(oversikthendelsetilfelle, callId)
                }
                else -> {
                    database.oppdaterPersonOppfolgingstilfelleMottatt(person.first().id, oversikthendelsetilfelle)
                    countOppdater(oversikthendelsetilfelle, callId)
                }
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

fun isInvalidOversikthendelsetilfelle(oversikthendelsetilfelle: KOversikthendelsetilfelle): Boolean {
    return oversikthendelsetilfelle.enhetId.length > 4
}

fun countOpprett(
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    callId: String = ""
) {
    if (oversikthendelsetilfelle.gradert) {
        LOG.info("Opprettet person basert pa gradert oversikthendelsetilfelle, for enhet {}, {}", oversikthendelsetilfelle.enhetId, callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT.inc()
    } else {
        LOG.info("Opprettet person basert pa oversikthendelsetilfelle med ingen aktivitet, for enhet {}, {}", oversikthendelsetilfelle.enhetId, callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT.inc()
    }
}

fun countOppdaterNyEnhet(
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    callId: String = ""
) {
    if (oversikthendelsetilfelle.gradert) {
        LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelle mottatt med ny enhet, {}", callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET.inc()
    } else {
        LOG.info("Oppdatert person basert pa oversikthendelsetilfelle med ingen aktivitet mottatt med ny enhet, {}", callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER_ENHET.inc()
    }
}

fun countOppdater(
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    callId: String = ""
) {
    if (oversikthendelsetilfelle.gradert) {
        LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelle mottatt, {}", callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER.inc()
    } else {
        LOG.info("Oppdatert person basert pa oversikthendelsetilfelle med ingen aktivitet mottatt, {}", callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER.inc()
    }
}
