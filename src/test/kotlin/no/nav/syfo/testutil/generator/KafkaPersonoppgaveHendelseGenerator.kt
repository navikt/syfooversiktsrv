package no.nav.syfo.testutil.generator

import no.nav.syfo.domain.OversikthendelseType
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import no.nav.syfo.testutil.UserConstants

fun generateKPersonoppgavehendelse(type: OversikthendelseType, personIdent: String = UserConstants.ARBEIDSTAKER_FNR) =
    KPersonoppgavehendelse(
        hendelsetype = type,
        personident = personIdent,
    )
