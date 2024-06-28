package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import java.util.*

fun generatePPersonOversiktStatus(fnr: String = UserConstants.ARBEIDSTAKER_FNR) = PPersonOversiktStatus(
    veilederIdent = null,
    fnr = fnr,
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
    aktivitetskravVurderingFrist = null,
    behandlerdialogSvarUbehandlet = false,
    behandlerdialogUbesvartUbehandlet = false,
    behandlerdialogAvvistUbehandlet = false,
    aktivitetskravVurderStansUbehandlet = false,
    trengerOppfolging = false,
    behandlerBerOmBistandUbehandlet = false,
    antallSykedager = null,
    friskmeldingTilArbeidsformidlingFom = null,
    isAktivArbeidsuforhetvurdering = false,
    isAktivSenOppfolgingKandidat = false,
)
