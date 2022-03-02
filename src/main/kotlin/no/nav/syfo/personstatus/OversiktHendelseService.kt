package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.metric.*
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.personstatus")

class OversiktHendelseService(
    private val database: DatabaseInterface,
) {
    fun oppdaterPersonMedHendelse(
        oversiktHendelse: KOversikthendelse,
        callId: String = "",
    ) {
        val oversikthendelseType = oversiktHendelse.oversikthendelseType()

        if (oversikthendelseType == null) {
            log.error(
                "Mottatt oversikthendelse med ukjent type, ${oversiktHendelse.hendelseId}, {}",
                callIdArgument(callId)
            )
            COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT.increment()
        } else {
            val personOversiktStatus = database.hentPersonResultat(
                fnr = oversiktHendelse.fnr,
            ).firstOrNull()?.toPersonOversiktStatus(oppfolgingstilfeller = emptyList())

            if (personOversiktStatus == null) {
                if (oversikthendelseType.isNotBehandling()) {
                    createPersonOversiktStatus(
                        oversiktHendelse = oversiktHendelse,
                        oversikthendelseType = oversikthendelseType,
                    )
                } else {
                    log.error(
                        "Fant ikke person som skal oppdateres med hendelse {}, for enhet {}, {}",
                        oversiktHendelse.hendelseId,
                        oversiktHendelse.enhetId,
                        callIdArgument(callId)
                    )
                    countFailed(
                        oversikthendelseType = oversikthendelseType,
                    )
                }
            } else {
                updatePersonOversiktStatus(
                    oversiktHendelse = oversiktHendelse,
                    oversikthendelseType = oversikthendelseType,
                    personOversiktStatus = personOversiktStatus,
                )
            }
        }
    }

    private fun createPersonOversiktStatus(
        oversiktHendelse: KOversikthendelse,
        oversikthendelseType: OversikthendelseType,
    ) {
        val personOversiktStatus = oversiktHendelse.toPersonOversiktStatus(
            oversikthendelseType = oversikthendelseType,
        )
        database.createPersonOversiktStatus(
            personOversiktStatus = personOversiktStatus,
        )
        countCreated(
            oversikthendelseType = oversikthendelseType,
        )
    }

    private fun updatePersonOversiktStatus(
        oversiktHendelse: KOversikthendelse,
        oversikthendelseType: OversikthendelseType,
        personOversiktStatus: PersonOversiktStatus,
    ) {
        val updatedPersonOversiktStatus = oversiktHendelse.toPersonOversiktStatus(
            oversikthendelseType = oversikthendelseType,
            personOversiktStatus = personOversiktStatus,
        )
        database.updatePersonOversiktStatus(
            personOversiktStatus = updatedPersonOversiktStatus,
        )
        countUpdated(
            oversikthendelseType = oversikthendelseType,
        )
    }

    private fun countCreated(
        oversikthendelseType: OversikthendelseType,
    ) {
        when (oversikthendelseType) {
            OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT.increment()
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPRETT.increment()
            else -> return
        }
    }

    private fun countFailed(
        oversikthendelseType: OversikthendelseType,
    ) {
        when (oversikthendelseType) {
            OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET.increment()
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_FEILET.increment()
            else -> return
        }
    }

    private fun countUpdated(
        oversikthendelseType: OversikthendelseType,
    ) {
        when (oversikthendelseType) {
            OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER.increment()
            OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER.increment()
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT -> COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER.increment()
            OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET -> COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPDATER.increment()
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_OPPDATER.increment()
        }
    }
}
