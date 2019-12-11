package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDate
import java.time.LocalDateTime


val generateOversikthendelsetilfelle =
        KOversikthendelsetilfelle(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                navn = UserConstants.ARBEIDSTAKER_NAVN,
                enhetId = UserConstants.NAV_ENHET,
                virksomhetsnummer = UserConstants.VIRKSOMHETSNUMMER,
                virksomhetsnavn = UserConstants.VIRKSOMHETSNAVN,
                gradert = false,
                fom = LocalDate.now().minusDays(56),
                tom = LocalDate.now().plusDays(16),
                tidspunkt = LocalDateTime.now()
        )
