package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import java.util.*

fun generatePPersonOversiktStatus() = PPersonOversiktStatus(
    veilederIdent = null,
    fnr = UserConstants.ARBEIDSTAKER_FNR,
    uuid = UUID.randomUUID(),
    navn = null,
    id = 1,
    enhet = null,
    tildeltEnhetUpdatedAt = null,
    motebehovUbehandlet = null,
    oppfolgingsplanLPSBistandUbehandlet = null,
    dialogmotesvarUbehandlet = false,
    dialogmotekandidat = null,
    dialogmotekandidatGeneratedAt = null,
    motestatus = null,
    motestatusGeneratedAt = null,
    oppfolgingstilfelleUpdatedAt = null,
    oppfolgingstilfelleGeneratedAt = null,
    oppfolgingstilfelleStart = null,
    oppfolgingstilfelleEnd = null,
    oppfolgingstilfelleBitReferanseUuid = null,
    oppfolgingstilfelleBitReferanseInntruffet = null,
    aktivitetskrav = null,
    aktivitetskravStoppunkt = null,
    aktivitetskravUpdatedAt = null,
    aktivitetskravVurderingFrist = null,
)
