package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.LOG
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.previouslyProcessed
import no.nav.syfo.personstatus.getPersonOversiktStatusList
import no.nav.syfo.util.callIdArgument

class OversikthendelstilfelleService(
    private val database: DatabaseInterface,
) {

    fun oppdaterPersonMedHendelse(
        oversikthendelsetilfelle: KOversikthendelsetilfelle,
        callId: String = ""
    ) {
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
                database.opprettEllerOppdaterOppfolgingstilfelle(
                    personId = pPersonOversiktStatusId,
                    oversikthendelsetilfelle = oversikthendelsetilfelle,
                )
                countOppdater(oversikthendelsetilfelle, callId)
            }
        }
    }
}

fun countOpprett(
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    callId: String = ""
) {
    if (oversikthendelsetilfelle.gradert) {
        LOG.info(
            "Opprettet person basert pa gradert oversikthendelsetilfelle, {}",
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT.increment()
    } else {
        LOG.info(
            "Opprettet person basert pa oversikthendelsetilfelle med ingen aktivitet, {}",
            callIdArgument(callId)
        )
        COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT.increment()
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
