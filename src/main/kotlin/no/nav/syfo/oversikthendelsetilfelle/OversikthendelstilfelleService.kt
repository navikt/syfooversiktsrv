package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.LOG
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.OversiktHendelseService.Companion.erPersonsEnhetOppdatert
import no.nav.syfo.personstatus.hentPersonResultat
import no.nav.syfo.util.CallIdArgument

class OversikthendelstilfelleService(private val database: DatabaseInterface) {

    fun oppdaterPersonMedHendelse(oversikthendelsetilfelle: KOversikthendelsetilfelle, callId: String = "") {
        if (oversikthendelsetilfelle.gradert) {
            oppdaterPersonMedHendelseOppfolgingstilfelleGradertMottatt(oversikthendelsetilfelle, callId)
        } else {
            oppdaterPersonMedHendelseOppfolgingstilfelleIngenAktivitetMottatt(oversikthendelsetilfelle, callId)
        }
    }

    private fun oppdaterPersonMedHendelseOppfolgingstilfelleIngenAktivitetMottatt(oversikthendelsetilfelle: KOversikthendelsetilfelle, callId: String) {
        val person = database.hentPersonResultat(oversikthendelsetilfelle.fnr)
        when {
            person.isEmpty() -> {
                LOG.info("Opprettet person basert pa oversikthendelsetilfelle med ingen aktivitet, for enhet {}, {}", oversikthendelsetilfelle.enhetId, CallIdArgument(callId))
                COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversikthendelsetilfelle.enhetId) -> {
                LOG.info("Oppdatert person basert pa oversikthendelsetilfelle med ingen aktivitet mottatt med ny enhet, {}", CallIdArgument(callId))
                COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER_ENHET.inc()
            }
            else -> {
                LOG.info("Oppdatert person basert pa oversikthendelsetilfelle med ingen aktivitet mottatt, {}", CallIdArgument(callId))
                COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseOppfolgingstilfelleGradertMottatt(oversikthendelsetilfelle: KOversikthendelsetilfelle, callId: String) {
        val person = database.hentPersonResultat(oversikthendelsetilfelle.fnr)
        when {
            person.isEmpty() -> {
                LOG.info("Opprettet person basert pa gradert oversikthendelsetilfelle, for enhet {}, {}", oversikthendelsetilfelle.enhetId, CallIdArgument(callId))
                COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversikthendelsetilfelle.enhetId) -> {
                LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelle mottatt med ny enhet, {}", CallIdArgument(callId))
                COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET.inc()
            }
            else -> {
                LOG.info("Oppdatert person basert pa gradert oversikthendelsetilfelle mottatt, {}", CallIdArgument(callId))
                COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER.inc()
            }
        }
    }
}
