package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.domain.KOversikthendelse
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDateTime

val generateOversikthendelse =
        KOversikthendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                UserConstants.NAV_ENHET,
                LocalDateTime.now()
        )
