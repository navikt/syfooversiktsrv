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
            else -> {
                log.error("Mottatt oversikthendelse med ukjent type, ${oversiktHendelse.hendelseId}")
                COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT.inc()
            }
        }
    }

    private fun oppdaterPersonMedHendelseMotebehovMottatt(oversiktHendelse: KOversikthendelse) {
        val person = database.hentPersonResultat(oversiktHendelse.fnr)
        when {
            person.isEmpty() -> {
                database.opprettPersonMedMotebehovMottatt(oversiktHendelse)
                log.info("Opprettet person basert pa oversikthendelse med motebehovsvar mottatt")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_MOTTATT_OPPRETT.inc()
            }
            erPersonsEnhetOppdatert(person, oversiktHendelse) -> {
                database.oppdaterPersonMedMotebehovMottattNyEnhet(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med motebehovsvar mottatt med ny enhet")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_MOTTATT_OPPDATER.inc()
            }
            else -> {
                database.oppdaterPersonMedMotebehovMottatt(oversiktHendelse)
                log.info("Oppdatert person basert pa oversikthendelse med motebehovsvar mottatt")
                COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_MOTTATT_OPPDATER_ENHET.inc()
            }
        }
    }

    fun erPersonsEnhetOppdatert(person: List<PersonOversiktStatus>, oversiktHendelse: KOversikthendelse): Boolean {
        val enhet = person[0].enhet
        val nyEnhet = oversiktHendelse.enhetId
        return nyEnhet.isNotEmpty() && nyEnhet != enhet
    }
}
