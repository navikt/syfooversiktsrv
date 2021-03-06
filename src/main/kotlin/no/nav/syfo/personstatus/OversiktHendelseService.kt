package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.personstatus")

class OversiktHendelseService(private val database: DatabaseInterface) {

    fun oppdaterPersonMedHendelse(oversiktHendelse: KOversikthendelse, callId: String = "") {
        when (oversiktHendelse.hendelseId) {
            OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.toString() -> oppdaterPersonMedHendelseMotebehovMottatt(oversiktHendelse, callId)
            OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.toString() -> oppdaterPersonMedHendelseMotebehovBehandlet(oversiktHendelse, callId)
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.toString() -> oppdaterPersonMedHendelseMoteplanleggerSvarMottat(oversiktHendelse, callId)
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.toString() -> oppdaterPersonMedHendelseMoteplanleggerSvarBehandlet(oversiktHendelse, callId)
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.toString() -> oppdaterPersonMedHendelseOppfolgingsplanLPSBistandMottatt(oversiktHendelse, callId)
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET.toString() -> oppdaterPersonMedHendelseOppfolgingsplanLPSBistandBehandlet(oversiktHendelse, callId)
            else -> {
                log.error("Mottatt oversikthendelse med ukjent type, ${oversiktHendelse.hendelseId}, {}", callIdArgument(callId))
                COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMoteplanleggerSvarMottat(oversiktHendelse: KOversikthendelse, callId: String) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                database.opprettPersonMedMoteplanleggerAlleSvarMottatt(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse.enhetId) -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarNyEnhet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarMottatt(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMoteplanleggerSvarBehandlet(oversiktHendelse: KOversikthendelse, callId: String) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                log.error("Fant ikke person som skal oppdateres med hendelse {}, for enhet {}", oversiktHendelse.hendelseId, oversiktHendelse.enhetId)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse.enhetId) -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarBehandletNyEnhet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarBehandlet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMotebehovBehandlet(oversiktHendelse: KOversikthendelse, callId: String) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                log.error("Fant ikke person som skal oppdateres med hendelse {}, for enhet {}, {}", oversiktHendelse.hendelseId, oversiktHendelse.enhetId, callIdArgument(callId))
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse.enhetId) -> {
                database.oppdaterPersonMedMotebehovBehandletNyEnhet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMotebehovBehandlet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMotebehovMottatt(oversiktHendelse: KOversikthendelse, callId: String) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                database.opprettPersonMedMotebehovMottatt(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse.enhetId) -> {
                database.oppdaterPersonMedMotebehovMottattNyEnhet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMotebehovMottatt(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseOppfolgingsplanLPSBistandMottatt(oversiktHendelse: KOversikthendelse, callId: String) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                database.opprettPersonMedOPLPSBistandMottatt(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse.enhetId) -> {
                database.oppdaterPersonMedOPLPSBistandMottattNyEnhet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedOPLPSBistandMottatt(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseOppfolgingsplanLPSBistandBehandlet(oversiktHendelse: KOversikthendelse, callId: String) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                log.error("Fant ikke person som skal oppdateres med hendelse {}, for enhet {}, {}", oversiktHendelse.hendelseId, oversiktHendelse.enhetId, callIdArgument(callId))
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse.enhetId) -> {
                database.oppdaterPersonMedOPLPSBistandBehandletNyEnhet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedOPLPSBistandBehandlet(oversiktHendelse)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET.inc()
            }
        }
    }

    companion object {

        fun erPersonsEnhetOppdatert(person: List<PPersonOversiktStatus>, nyEnhetId: String): Boolean {
            val enhet = person[0].enhet
            return nyEnhetId.isNotEmpty() && nyEnhetId != enhet
        }
    }
}
