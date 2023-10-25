package no.nav.syfo.testutil.generator

import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.testutil.UserConstants

fun generateKPersonoppgavehendelse(type: OversikthendelseType, personIdent: String = UserConstants.ARBEIDSTAKER_FNR) = KPersonoppgavehendelse(
    hendelsetype = type,
    personident = personIdent,
)
