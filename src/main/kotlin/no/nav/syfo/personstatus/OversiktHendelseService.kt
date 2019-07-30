package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.personstatus.domain.*
import org.slf4j.LoggerFactory

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.personstatus")

class OversiktHendelseService(private val database: DatabaseInterface) {

    fun oppdaterPersonMedHendelse(oversiktHendelse: KOversikthendelse) {
        when (oversiktHendelse.hendelseId) {
            OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.toString() -> oppdaterPersonMedHendelseMotebehovMottatt(oversiktHendelse)
            OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.toString() -> oppdaterPersonMedHendelseMotebehovBehandlet(oversiktHendelse)
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.toString() -> oppdaterPersonMedHendelseMoteplanleggerSvarMottat(oversiktHendelse)
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.toString() -> oppdaterPersonMedHendelseMoteplanleggerSvarBehandlet(oversiktHendelse)
            else -> {
                log.error("Mottatt oversikthendelse med ukjent type, ${oversiktHendelse.hendelseId}")
                COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMoteplanleggerSvarMottat(oversiktHendelse: KOversikthendelse) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                database.opprettPersonMedMoteplanleggerAlleSvarMottatt(oversiktHendelse)
                log.info("Opprettet person basert pa oversikthendelse med moteplanlegger alle svar mottatt")
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse) -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarNyEnhet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med moteplanlegger alle svar mottatt med ny enhet")
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarMottatt(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med moteplanlegger alle svar mottatt")
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMoteplanleggerSvarBehandlet(oversiktHendelse: KOversikthendelse) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                log.error("Fant ikke person som skal oppdateres med hendelse {}, for enhet {}", oversiktHendelse.hendelseId, oversiktHendelse.enhetId)
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse) -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarBehandletNyEnhet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med moteplanleggersvar behandlet med ny enhet")
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMoteplanleggerAlleSvarBehandlet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med moteplanleggersvar behandlet")
                COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMotebehovBehandlet(oversiktHendelse: KOversikthendelse) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                log.error("Fant ikke person som skal oppdateres med hendelse {}, for enhet {}", oversiktHendelse.hendelseId, oversiktHendelse.enhetId)
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse) -> {
                database.oppdaterPersonMedMotebehovBehandletNyEnhet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med motebehovsvar behandlet med ny enhet")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMotebehovBehandlet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med motebehovsvar behandlet")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMotebehovMottatt(oversiktHendelse: KOversikthendelse) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                database.opprettPersonMedMotebehovMottatt(oversiktHendelse)
                log.info("Opprettet person basert pa oversikthendelse med motebehovsvar mottatt")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse) -> {
                database.oppdaterPersonMedMotebehovMottattNyEnhet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med motebehovsvar mottatt med ny enhet")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET.inc()
            }
            else -> {
                database.oppdaterPersonMedMotebehovMottatt(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med motebehovsvar mottatt")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER.inc()
            }
        }
    }

    fun erPersonsEnhetOppdatert(person: List<PersonOversiktStatus>, oversiktHendelse: KOversikthendelse): Boolean {
        val enhet = person[0].enhet
        val nyEnhet = oversiktHendelse.enhetId
        return nyEnhet.isNotEmpty() && nyEnhet != enhet
    }
}
