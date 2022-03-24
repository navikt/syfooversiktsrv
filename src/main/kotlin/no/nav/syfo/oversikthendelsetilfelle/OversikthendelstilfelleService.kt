package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.LOG
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.previouslyProcessed
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.getPersonOversiktStatusList
import no.nav.syfo.util.callIdArgument

class OversikthendelstilfelleService(
    private val database: DatabaseInterface,
) {

    fun oppdaterPersonMedHendelse(
        oversikthendelsetilfelle: KOversikthendelsetilfelle,
        callId: String = ""
    ) {
        if (isInvalidOversikthendelsetilfelle(oversikthendelsetilfelle)) {
            LOG.error(
                "Oppdaterte ikke person med oversikthendelsetilfelle med ugyldig EnhetId, for enhet {}, {}",
                oversikthendelsetilfelle.enhetId,
                callIdArgument(callId)
            )
            COUNT_OVERSIKTHENDELSETILFELLE_UGDYLGIG_MOTTATT.increment()
            return
        } else {
            val pPersonOversiktStatus = database.getPersonOversiktStatusList(oversikthendelsetilfelle.fnr)

            if (pPersonOversiktStatus.isEmpty()) {
                database.opprettPersonOppfolgingstilfelleMottatt(oversikthendelsetilfelle)
                countOpprett(oversikthendelsetilfelle, callId)
            } else {
                val pPersonOversiktStatusId = pPersonOversiktStatus.first().id
                val pPersonOppfolgingstilfelleList = database.hentOppfolgingstilfelleResultat(
                    personId = pPersonOversiktStatusId,
                    virksomhetsnummer = oversikthendelsetilfelle.virksomhetsnummer,
                )
                val isOppfolgingstilfellePreviouslyProcessed = oversikthendelsetilfelle.previouslyProcessed(
                    lastUpdatedAt = pPersonOppfolgingstilfelleList.firstOrNull()?.sistEndret
                )
                if (isOppfolgingstilfellePreviouslyProcessed) {
                    LOG.info("Hopper over KOversikthendelsetilfelle med tidspunkt=${oversikthendelsetilfelle.tidspunkt}: Allerede mottatt og prossesert KOversikthendelsetilfelle")
                } else {
                    if (erPersonsEnhetOppdatert(pPersonOversiktStatus, oversikthendelsetilfelle.enhetId)) {
                        database.oppdaterPersonOppfolgingstilfelleNyEnhetMottatt(
                            personId = pPersonOversiktStatusId,
                            oversikthendelsetilfelle = oversikthendelsetilfelle,
                        )
                        countOppdaterNyEnhet(oversikthendelsetilfelle, callId)
                    } else {
                        database.oppdaterPersonOppfolgingstilfelleMottatt(
                            personId = pPersonOversiktStatusId,
                            oversikthendelsetilfelle = oversikthendelsetilfelle,
                        )
                        countOppdater(oversikthendelsetilfelle, callId)
                    }
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
        LOG.info(
            "Opprettet person basert pa gradert oversikthendelsetilfelle, for enhet {}, {}",
            oversikthendelsetilfelle.enhetId,
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT.increment()
    } else {
        LOG.info(
            "Opprettet person basert pa oversikthendelsetilfelle med ingen aktivitet, for enhet {}, {}",
            oversikthendelsetilfelle.enhetId,
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT.increment()
    }
}

fun countOppdaterNyEnhet(
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    callId: String = ""
) {
    if (oversikthendelsetilfelle.gradert) {
        LOG.info(
            "Oppdatert person basert pa gradert oversikthendelsetilfelle mottatt med ny enhet, {}",
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET.increment()
    } else {
        LOG.info(
            "Oppdatert person basert pa oversikthendelsetilfelle med ingen aktivitet mottatt med ny enhet, {}",
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER_ENHET.increment()
    }
}

fun countOppdater(
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    callId: String = ""
) {
    if (oversikthendelsetilfelle.gradert) {
        LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelle mottatt, {}", callIdArgument(callId))
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER.increment()
    } else {
        LOG.info(
            "Oppdatert person basert pa oversikthendelsetilfelle med ingen aktivitet mottatt, {}",
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER.increment()
    }
}
